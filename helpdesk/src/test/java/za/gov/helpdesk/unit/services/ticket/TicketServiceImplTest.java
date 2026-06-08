package za.gov.helpdesk.unit.services.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.event.TicketEventDispatcher;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.metrics.TicketMetrics;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.policy.TicketStatusTransitionPolicy;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.impl.TicketServiceImpl;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImplTest unit tests")
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private TicketEventDispatcher eventDispatcher;
    @Mock
    private TicketStatusTransitionPolicy transitionPolicy;
    @Mock
    private SlaService slaService;
    @Mock
    private TicketMetrics ticketMetrics;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private User agentUser;
    private User endUser;
    private Agent agent;
    private Ticket openTicket;

    @BeforeEach
    void setUp() {
        agentUser = User.builder().id(1L).name("Jane Agent").email("jane@gov.za")
                .role(User.Role.AGENT).active(true).build();
        endUser = User.builder().id(2L).name("John Public").email("john@citizen.za")
                .role(User.Role.USER).active(true).build();
        agent = Agent.builder().id(1L).user(agentUser)
                .availability(Agent.Availability.ONLINE).build();
        openTicket = Ticket.builder()
                .id(100L).subject("Login broken").description("Cannot access dashboard")
                .status(Ticket.Status.OPEN).priority(Ticket.Priority.HIGH)
                .requester(endUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createTicket() persists ticket, initialises SLA, and dispatches TICKET_CREATED event")
    void createTicket_validRequest_savesInitializesSlaAndDispatchesEvent() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Login broken");
        req.setDescription("Cannot access dashboard");
        req.setPriority(Ticket.Priority.HIGH);

        given(ticketRepository.save(any(Ticket.class))).willReturn(openTicket);
        given(ticketMapper.toTicketResponse(openTicket)).willReturn(responseFor(openTicket));

        TicketResponse response = ticketService.createTicket(req, endUser);

        assertThat(response.getSubject()).isEqualTo("Login broken");
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.OPEN);

        then(ticketMetrics).should(times(1)).incrementCreated();
        then(ticketRepository).should(times(1)).save(any(Ticket.class));
        then(slaService).should(times(1)).initializeSla(openTicket);
        then(eventDispatcher).should(times(1)).publish(
                eq(openTicket), eq(endUser),
                eq(AuditLog.AuditAction.TICKET_CREATED),
                eq(null), eq(Ticket.Status.OPEN.name()),
                any(), eq(null)
        );
    }

    @Test
    @DisplayName("createTicket() defaults priority to MEDIUM when not specified")
    void createTicket_noPriority_defaultsMedium() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Test");
        req.setDescription("Test desc");

        Ticket mediumTicket = Ticket.builder()
                .id(101L).subject("Test").description("Test desc")
                .status(Ticket.Status.OPEN).priority(Ticket.Priority.MEDIUM)
                .requester(endUser).build();

        given(ticketRepository.save(any(Ticket.class))).willReturn(mediumTicket);
        given(ticketMapper.toTicketResponse(mediumTicket)).willReturn(responseFor(mediumTicket));

        TicketResponse response = ticketService.createTicket(req, endUser);

        assertThat(response.getPriority()).isEqualTo(Ticket.Priority.MEDIUM);
    }

    @Test
    @DisplayName("createTicket() with assigneeId looks up agent and dispatches ASSIGNED_TO_AGENT")
    void createTicket_withAssigneeId_loadsAgentAndPublishesAssignedEvent() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Assigned ticket");
        req.setDescription("desc");
        req.setAssigneeId(1L);

        Ticket ticketWithAssignee = Ticket.builder()
                .id(102L).subject("Assigned ticket").description("desc")
                .status(Ticket.Status.OPEN).priority(Ticket.Priority.MEDIUM)
                .requester(endUser).assignee(agent).build();

        given(agentRepository.findById(1L)).willReturn(Optional.of(agent));
        given(ticketRepository.save(any(Ticket.class))).willReturn(ticketWithAssignee);
        given(ticketMapper.toTicketResponse(ticketWithAssignee)).willReturn(responseFor(ticketWithAssignee));

        ticketService.createTicket(req, endUser);

        then(eventDispatcher).should(times(2)).publish(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createTicket() throws ResourceNotFoundException for unknown assignee")
    void createTicket_unknownAssignee_throwsNotFound() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Test");
        req.setDescription("desc");
        req.setAssigneeId(999L);

        given(agentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(req, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getTicketById() returns response for authorised principal")
    void getTicketById_authorised_returnsResponse() {
        given(ticketRepository.findByIdAndPrincipal(100L, endUser.getEmail(), endUser.getRole().name()))
                .willReturn(Optional.of(openTicket));
        given(ticketMapper.toTicketResponse(openTicket)).willReturn(responseFor(openTicket));

        TicketResponse response = ticketService.getTicketById(100L, endUser);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getTicketById() throws ResourceNotFoundException for unknown ID")
    void getTicketById_unknownId_throwsNotFound() {
        given(ticketRepository.findByIdAndPrincipal(999L, endUser.getEmail(), endUser.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(999L, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateTicket() OPEN -> IN_PROGRESS records first SLA response")
    void updateTicket_openToInProgress_recordsFirstResponse() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.IN_PROGRESS);

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.IN_PROGRESS);
        then(slaService).should(times(1)).recordFirstResponse(100L);
        then(slaService).should(never()).recordResolution(anyLong());
        then(eventDispatcher).should(times(1)).publish(
                eq(openTicket), eq(agentUser),
                eq(AuditLog.AuditAction.STATUS_CHANGED),
                eq("OPEN"), eq("IN_PROGRESS"), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() IN_PROGRESS -> RESOLVED records SLA resolution")
    void updateTicket_inProgressToResolved_recordsResolution() {
        Ticket inProgress = Ticket.builder()
                .id(100L).subject("Login broken").description("desc")
                .status(Ticket.Status.IN_PROGRESS).priority(Ticket.Priority.HIGH)
                .requester(endUser).assignee(agent).build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.RESOLVED);

        givenAuthorizedTicket(100L, agentUser, inProgress);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        then(ticketMetrics).should(times(1)).incrementResolved();

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.RESOLVED);
        then(slaService).should(times(1)).recordResolution(100L);
        then(slaService).should(never()).recordFirstResponse(anyLong());
    }

    @Test
    @DisplayName("updateTicket() RESOLVED -> CLOSED dispatches TICKET_CLOSED event")
    void updateTicket_resolvedToClosed_dispatchesTicketClosed() {
        Ticket resolved = Ticket.builder()
                .id(100L).subject("Login broken").description("desc")
                .status(Ticket.Status.RESOLVED).priority(Ticket.Priority.HIGH)
                .requester(endUser).assignee(agent).build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.CLOSED);

        givenAuthorizedTicket(100L, agentUser, resolved);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        then(ticketMetrics).should(times(1)).incrementClosed();

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.CLOSED);
        then(eventDispatcher).should(times(1)).publish(
                eq(resolved), eq(agentUser),
                eq(AuditLog.AuditAction.TICKET_CLOSED),
                eq("RESOLVED"), eq("CLOSED"), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() CLOSED -> OPEN throws InvalidStatusTransitionException and never saves")
    void updateTicket_invalidTransition_throwsAndNeverSaves() {
        Ticket closed = Ticket.builder()
                .id(200L).subject("Old issue").description("Done")
                .status(Ticket.Status.CLOSED)
                .requester(endUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.OPEN);

        givenAuthorizedTicket(200L, agentUser, closed);
        // real policy so the exception is actually thrown
        willThrow(new InvalidStatusTransitionException(Ticket.Status.CLOSED, Ticket.Status.OPEN))
                .given(transitionPolicy).assertCanTransition(Ticket.Status.CLOSED, Ticket.Status.OPEN);

        assertThatThrownBy(() -> ticketService.updateTicket(200L, req, agentUser))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("OPEN");

        then(ticketRepository).should(never()).save(any());
        then(eventDispatcher).should(never()).publish(any(), any(), any(), any(), any(), any(), any());
    }


    @Test
    @DisplayName("updateTicket() assigns agent and dispatches ASSIGNED_TO_AGENT event")
    void updateTicket_assignAgent_dispatchesAssignedEvent() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(agentRepository.findById(1L)).willReturn(Optional.of(agent));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(eventDispatcher).should(times(1)).publish(
                eq(openTicket), eq(agentUser),
                eq(AuditLog.AuditAction.ASSIGNED_TO_AGENT),
                eq("Unassigned"), eq(agentUser.getName()), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() skips assignment when agent is already assigned")
    void updateTicket_sameAssignee_noEventDispatched() {
        Ticket assigned = Ticket.builder()
                .id(100L).subject("Login broken").description("desc")
                .status(Ticket.Status.OPEN).priority(Ticket.Priority.HIGH)
                .requester(endUser).assignee(agent).build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        givenAuthorizedTicket(100L, agentUser, assigned);
        given(agentRepository.findById(1L)).willReturn(Optional.of(agent));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(eventDispatcher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateTicket() priority change dispatches PRIORITY_CHANGED event")
    void updateTicket_priorityChange_dispatchesPriorityChanged() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setPriority(Ticket.Priority.URGENT);

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        assertThat(response.getPriority()).isEqualTo(Ticket.Priority.URGENT);
        then(eventDispatcher).should(times(1)).publish(
                eq(openTicket), eq(agentUser),
                eq(AuditLog.AuditAction.PRIORITY_CHANGED),
                eq("HIGH"), eq("URGENT"), eq(null), eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() no-op when priority is unchanged")
    void updateTicket_samePriority_noEventDispatched() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setPriority(Ticket.Priority.HIGH); // same as openTicket

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(eventDispatcher).shouldHaveNoInteractions();
    }


    @Test
    @DisplayName("updateTicket() escalation from IN_PROGRESS sets ESCALATED status")
    void updateTicket_escalationFromInProgress_marksEscalatedStatus() {
        Ticket inProgress = Ticket.builder()
                .id(100L).subject("Login broken").description("desc")
                .status(Ticket.Status.IN_PROGRESS).priority(Ticket.Priority.HIGH)
                .requester(endUser).assignee(agent).build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setEscalated(true);
        req.setEscalationReason("SLA risk");

        givenAuthorizedTicket(100L, agentUser, inProgress);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        then(ticketMetrics).should(times(1)).incrementEscalated();

        assertThat(response.isEscalated()).isTrue();
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.ESCALATED);
        then(eventDispatcher).should(times(1)).publish(
                eq(inProgress), eq(agentUser),
                eq(AuditLog.AuditAction.ESCALATED),
                eq("false"), eq("true"), eq("SLA risk"), eq("SLA risk")
        );
    }

    @Test
    @DisplayName("updateTicket() escalation on already-escalated ticket is a no-op")
    void updateTicket_alreadyEscalated_noEventDispatched() {
        Ticket alreadyEscalated = Ticket.builder()
                .id(100L).subject("Login broken").description("desc")
                .status(Ticket.Status.ESCALATED).priority(Ticket.Priority.HIGH)
                .requester(endUser).escalated(true).build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setEscalated(true);
        req.setEscalationReason("Again?");

        givenAuthorizedTicket(100L, agentUser, alreadyEscalated);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(eventDispatcher).shouldHaveNoInteractions();
    }


    @Test
    @DisplayName("deleteTicket() dispatches TICKET_DELETED and removes from repository")
    void deleteTicket_valid_dispatchesEventAndDeletes() {
        given(ticketRepository.findById(100L)).willReturn(Optional.of(openTicket));

        ticketService.deleteTicket(100L, agentUser);

        then(eventDispatcher).should(times(1)).publish(
                eq(openTicket), eq(agentUser),
                eq(AuditLog.AuditAction.TICKET_DELETED),
                eq("OPEN"), eq("DELETED"),
                any(), eq(null)
        );
        then(ticketRepository).should(times(1)).delete(openTicket);
    }

    @Test
    @DisplayName("deleteTicket() throws ResourceNotFoundException for unknown ID")
    void deleteTicket_unknownId_throwsNotFound() {
        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.deleteTicket(999L, agentUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenAuthorizedTicket(Long ticketId, User actor, Ticket ticket) {
        given(ticketRepository.findByIdAndPrincipal(ticketId, actor.getEmail(), actor.getRole().name()))
                .willReturn(Optional.of(ticket));
    }

    private TicketResponse responseFor(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .escalated(ticket.isEscalated())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
