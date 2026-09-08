package za.gov.helpdesk.unit.services.ticket;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.event.TicketChangeEvent;
import za.gov.helpdesk.ticket.event.TicketEventDispatcher;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.metrics.TicketMetrics;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.policy.TicketStatusTransitionPolicy;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.impl.CategoryRoutingService;
import za.gov.helpdesk.ticket.service.impl.TicketUpdateCoordinator;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketUpdateCoordinator unit tests")
class TicketUpdateCoordinatorTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private AgentQueryHelper agentQuery;
    @Mock private CategoryQueryHelper categoryQuery;
    @Mock private CategoryRoutingService categoryRoutingService;
    @Mock private TicketEventDispatcher eventDispatcher;
    @Mock private SlaService slaService;
    @Mock private TicketMetrics ticketMetrics;
    @Captor private ArgumentCaptor<TicketChangeEvent> eventCaptor;

    private final TicketStatusTransitionPolicy transitionPolicy =
            new TicketStatusTransitionPolicy();

    private TicketUpdateCoordinator coordinator;
    private User actor;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        coordinator =
                new TicketUpdateCoordinator(
                        ticketRepository,
                        agentQuery,
                        categoryQuery,
                        categoryRoutingService,
                        eventDispatcher,
                        transitionPolicy,
                        slaService,
                        ticketMetrics);

        actor = User.builder().id(1L).name("Jane Agent").email("jane@gov.za").build();
        ticket =
                Ticket.builder()
                        .id(100L)
                        .subject("Printer broken")
                        .status(Status.OPEN)
                        .priority(Priority.MEDIUM)
                        .build();
    }

    // ---- handlePostCreation ----

    @Test
    @DisplayName(
            "handlePostCreation() initialises SLA, increments the created counter, and publishes a"
                    + " creation event")
    void handlePostCreation_alreadyAssigned_doesNotAutoRoute() {
        final Agent existingAssignee = Agent.builder().id(5L).build();
        ticket.setAssignee(existingAssignee);

        coordinator.handlePostCreation(ticket, actor);

        then(slaService).should(times(1)).initializeSla(ticket);
        then(ticketMetrics).should(times(1)).incrementCreated();
        then(categoryRoutingService).should(never()).resolveAssignee(any());

        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        final TicketChangeEvent event = eventCaptor.getValue();
        assertThat(event.action()).isEqualTo(AuditLog.AuditAction.TICKET_CREATED);
        assertThat(event.newValue()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName(
            "handlePostCreation() auto-routes an unassigned ticket when a candidate agent is found")
    void handlePostCreation_unassignedWithRoutableCategory_autoRoutes() {
        final Category category =
                Category.builder().id(9L).name("Hardware").defaultDepartment("IT Support").build();
        ticket.setCategory(category);
        final User agentUser = User.builder().id(2L).name("Agent Smith").build();
        final Agent agent = Agent.builder().id(7L).user(agentUser).build();

        given(categoryRoutingService.resolveAssignee(category)).willReturn(Optional.of(agent));

        coordinator.handlePostCreation(ticket, actor);

        assertThat(ticket.getAssignee()).isEqualTo(agent);
        then(ticketRepository).should(times(1)).save(ticket);
        // one publish for TICKET_CREATED, one for the auto-routing ASSIGNED_TO_AGENT event
        then(eventDispatcher).should(times(2)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(TicketChangeEvent::action)
                .containsExactly(
                        AuditLog.AuditAction.TICKET_CREATED,
                        AuditLog.AuditAction.ASSIGNED_TO_AGENT);
    }

    @Test
    @DisplayName(
            "handlePostCreation() leaves an unassigned ticket alone when no routing candidate is"
                    + " found")
    void handlePostCreation_unassignedNoCandidate_staysUnassigned() {
        final Category category =
                Category.builder().id(9L).name("Hardware").defaultDepartment("IT Support").build();
        ticket.setCategory(category);
        given(categoryRoutingService.resolveAssignee(category)).willReturn(Optional.empty());

        coordinator.handlePostCreation(ticket, actor);

        assertThat(ticket.getAssignee()).isNull();
        then(ticketRepository).should(never()).save(any());
        then(eventDispatcher).should(times(1)).publish(any());
    }

    // ---- handleDeletion ----

    @Test
    @DisplayName("handleDeletion() publishes a deletion audit event and deletes the ticket")
    void handleDeletion_anyTicket_publishesThenDeletes() {
        coordinator.handleDeletion(ticket, actor);

        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo(AuditLog.AuditAction.TICKET_DELETED);
        then(ticketRepository).should(times(1)).delete(ticket);
    }

    // ---- applyStatusChange ----

    @Test
    @DisplayName("applyStatusChange() to IN_PROGRESS records the SLA first-response timestamp")
    void applyStatusChange_toInProgress_recordsFirstResponse() {
        coordinator.applyStatusChange(ticket, Status.IN_PROGRESS, actor);

        assertThat(ticket.getStatus()).isEqualTo(Status.IN_PROGRESS);
        then(slaService).should(times(1)).recordFirstResponse(100L);
        then(ticketMetrics).should(never()).incrementResolved();
    }

    @Test
    @DisplayName(
            "applyStatusChange() to RESOLVED records resolution, increments counters, and records"
                    + " timing")
    void applyStatusChange_toResolved_recordsResolutionAndMetrics() {
        ticket.setStatus(Status.IN_PROGRESS);
        ticket.setCreatedAt(LocalDateTime.now().minusHours(2));

        coordinator.applyStatusChange(ticket, Status.RESOLVED, actor);

        then(slaService).should(times(1)).recordResolution(100L);
        then(ticketMetrics).should(times(1)).incrementResolved();
        then(ticketMetrics).should(times(1)).recordResolutionTime(ticket.getCreatedAt());
    }

    @Test
    @DisplayName("applyStatusChange() to RESOLVED skips timing metrics when createdAt is unset")
    void applyStatusChange_toResolvedNoCreatedAt_skipsTimingMetric() {
        ticket.setStatus(Status.IN_PROGRESS);
        ticket.setCreatedAt(null);

        coordinator.applyStatusChange(ticket, Status.RESOLVED, actor);

        then(ticketMetrics).should(never()).recordResolutionTime(any());
    }

    @Test
    @DisplayName(
            "applyStatusChange() to CLOSED increments the closed counter and fires a TICKET_CLOSED"
                    + " event")
    void applyStatusChange_toClosed_incrementsClosedCounterAndFiresClosedEvent() {
        ticket.setStatus(Status.RESOLVED);

        coordinator.applyStatusChange(ticket, Status.CLOSED, actor);

        then(ticketMetrics).should(times(1)).incrementClosed();
        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo(AuditLog.AuditAction.TICKET_CLOSED);
    }

    @Test
    @DisplayName("applyStatusChange() to a non-CLOSED status fires a STATUS_CHANGED event")
    void applyStatusChange_toInProgress_firesStatusChangedEvent() {
        coordinator.applyStatusChange(ticket, Status.IN_PROGRESS, actor);

        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo(AuditLog.AuditAction.STATUS_CHANGED);
    }

    @Test
    @DisplayName(
            "applyStatusChange() to ESCALATED flags the ticket and increments the escalated"
                    + " counter")
    void applyStatusChange_toEscalated_flagsTicketAndIncrementsCounter() {
        ticket.setStatus(Status.IN_PROGRESS);

        coordinator.applyStatusChange(ticket, Status.ESCALATED, actor);

        assertThat(ticket.isEscalated()).isTrue();
        then(ticketMetrics).should(times(1)).incrementEscalated();
    }

    @Test
    @DisplayName(
            "applyStatusChange() enforces the transition policy and rejects illegal transitions")
    void applyStatusChange_illegalTransition_throwsAndMakesNoChanges() {
        ticket.setStatus(Status.CLOSED);

        assertThatThrownBy(() -> coordinator.applyStatusChange(ticket, Status.OPEN, actor))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(ticket.getStatus()).isEqualTo(Status.CLOSED);
        then(eventDispatcher).should(never()).publish(any());
    }

    // ---- applyAssignmentChange ----

    @Test
    @DisplayName(
            "applyAssignmentChange() assigns a new agent and publishes an ASSIGNED_TO_AGENT event")
    void applyAssignmentChange_newAgent_assignsAndPublishes() {
        final User agentUser = User.builder().id(3L).name("Agent Smith").build();
        final Agent newAgent = Agent.builder().id(7L).user(agentUser).build();
        given(agentQuery.findOrThrow(7L)).willReturn(newAgent);

        coordinator.applyAssignmentChange(ticket, 7L, actor);

        assertThat(ticket.getAssignee()).isEqualTo(newAgent);
        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().oldValue()).isEqualTo("Unassigned");
        assertThat(eventCaptor.getValue().newValue()).isEqualTo("Agent Smith");
    }

    @Test
    @DisplayName("applyAssignmentChange() records the prior assignee's name when reassigning")
    void applyAssignmentChange_reassignFromExistingAgent_recordsOldAssigneeName() {
        final User oldAgentUser = User.builder().id(2L).name("Old Agent").build();
        final Agent oldAgent = Agent.builder().id(5L).user(oldAgentUser).build();
        ticket.setAssignee(oldAgent);

        final User newAgentUser = User.builder().id(3L).name("New Agent").build();
        final Agent newAgent = Agent.builder().id(7L).user(newAgentUser).build();
        given(agentQuery.findOrThrow(7L)).willReturn(newAgent);

        coordinator.applyAssignmentChange(ticket, 7L, actor);

        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().oldValue()).isEqualTo("Old Agent");
    }

    @Test
    @DisplayName(
            "applyAssignmentChange() is a no-op when reassigning to the already-assigned agent")
    void applyAssignmentChange_sameAgent_noOp() {
        final Agent sameAgent = Agent.builder().id(7L).build();
        ticket.setAssignee(sameAgent);
        given(agentQuery.findOrThrow(7L)).willReturn(sameAgent);

        coordinator.applyAssignmentChange(ticket, 7L, actor);

        then(eventDispatcher).should(never()).publish(any());
    }

    // ---- applyPriorityChange ----

    @Test
    @DisplayName(
            "applyPriorityChange() updates the priority and publishes a PRIORITY_CHANGED event")
    void applyPriorityChange_newPriority_updatesAndPublishes() {
        coordinator.applyPriorityChange(ticket, Priority.URGENT, actor);

        assertThat(ticket.getPriority()).isEqualTo(Priority.URGENT);
        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        final TicketChangeEvent event = eventCaptor.getValue();
        assertThat(event.action()).isEqualTo(AuditLog.AuditAction.PRIORITY_CHANGED);
        assertThat(event.oldValue()).isEqualTo("MEDIUM");
        assertThat(event.newValue()).isEqualTo("URGENT");
    }

    // ---- applyCategoryChange ----

    @Test
    @DisplayName("applyCategoryChange() moves the ticket to a new category and publishes an event")
    void applyCategoryChange_newCategory_movesAndPublishes() {
        final Category newCategory = Category.builder().id(9L).name("Software").build();
        given(categoryQuery.findOrThrow(9L)).willReturn(newCategory);

        coordinator.applyCategoryChange(ticket, 9L, actor);

        assertThat(ticket.getCategory()).isEqualTo(newCategory);
        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().oldValue()).isEqualTo("Uncategorised");
        assertThat(eventCaptor.getValue().newValue()).isEqualTo("Software");
    }

    @Test
    @DisplayName("applyCategoryChange() records the prior category name when recategorising")
    void applyCategoryChange_fromExistingCategory_recordsOldCategoryName() {
        final Category oldCategory = Category.builder().id(1L).name("Hardware").build();
        ticket.setCategory(oldCategory);
        final Category newCategory = Category.builder().id(9L).name("Software").build();
        given(categoryQuery.findOrThrow(9L)).willReturn(newCategory);

        coordinator.applyCategoryChange(ticket, 9L, actor);

        then(eventDispatcher).should(times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().oldValue()).isEqualTo("Hardware");
    }

    @Test
    @DisplayName("applyCategoryChange() is a no-op when moving to the already-assigned category")
    void applyCategoryChange_sameCategory_noOp() {
        final Category sameCategory = Category.builder().id(9L).name("Software").build();
        ticket.setCategory(sameCategory);
        given(categoryQuery.findOrThrow(9L)).willReturn(sameCategory);

        coordinator.applyCategoryChange(ticket, 9L, actor);

        then(eventDispatcher).should(never()).publish(any());
    }

    // ---- applyEscalation ----

    @Test
    @DisplayName(
            "applyEscalation() flags the ticket, transitions status, and publishes an ESCALATED"
                    + " event")
    void applyEscalation_openTicket_flagsTransitionsAndPublishes() {
        ticket.setStatus(Status.IN_PROGRESS);

        coordinator.applyEscalation(ticket, "SLA breach imminent", actor);

        assertThat(ticket.isEscalated()).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(Status.ESCALATED);
        // applyStatusChange publishes a STATUS_CHANGED-family event, then applyEscalation publishes
        // its own dedicated ESCALATED event - two publishes in total.
        then(eventDispatcher).should(times(2)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(TicketChangeEvent::action)
                .contains(AuditLog.AuditAction.ESCALATED);
        assertThat(eventCaptor.getAllValues().get(1).comment()).isEqualTo("SLA breach imminent");
    }
}
