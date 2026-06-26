package za.gov.helpdesk.unit.services.ticket;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.ticket.service.impl.TicketServiceImpl;
import za.gov.helpdesk.ticket.service.impl.TicketUpdateCoordinator;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImplTest unit tests")
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketQueryHelper ticketQuery;
    @Mock private AgentQueryHelper agentQuery;
    @Mock private TicketMapper ticketMapper;
    @Mock private TicketUpdateCoordinator updateCoordinator;

    @InjectMocks private TicketServiceImpl ticketService;

    private User agentUser;
    private User endUser;
    private User adminUser;
    private Agent agent;
    private Ticket openTicket;

    @BeforeEach
    void setUp() {
        agentUser =
                User.builder()
                        .id(1L)
                        .name("Jane Agent")
                        .email("jane@gov.za")
                        .role(User.Role.AGENT)
                        .active(true)
                        .build();

        endUser =
                User.builder()
                        .id(2L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(User.Role.USER)
                        .active(true)
                        .build();

        adminUser =
                User.builder()
                        .id(3L)
                        .name("T Mofo")
                        .email("tmofo@citizen.za")
                        .role(User.Role.ADMIN)
                        .active(true)
                        .build();

        agent =
                Agent.builder()
                        .id(1L)
                        .user(agentUser)
                        .availability(Agent.Availability.ONLINE)
                        .build();

        openTicket =
                Ticket.builder()
                        .id(100L)
                        .subject("Login broken")
                        .description("Cannot access dashboard")
                        .status(Ticket.Status.OPEN)
                        .priority(Ticket.Priority.HIGH)
                        .requester(endUser)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
    }

    @Test
    @DisplayName("createTicket() persists ticket, delegates to coordinator, and returns mapping")
    void createTicket_validRequest_savesInitializesSlaAndDispatchesEvent() {
        final CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Login broken");
        req.setDescription("Cannot access dashboard");
        req.setPriority(Ticket.Priority.HIGH);

        given(ticketRepository.save(any(Ticket.class))).willReturn(openTicket);
        given(ticketMapper.toTicketResponse(openTicket)).willReturn(responseFor(openTicket));

        final TicketResponse response = ticketService.createTicket(req, endUser);

        assertThat(response.getSubject()).isEqualTo("Login broken");
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.OPEN);

        then(ticketRepository).should(times(1)).save(any(Ticket.class));
        then(updateCoordinator).should(times(1)).handlePostCreation(openTicket, endUser);
    }

    @Test
    @DisplayName("createTicket() defaults priority to MEDIUM when not specified")
    void createTicket_noPriority_defaultsMedium() {
        final CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Test");
        req.setDescription("Test desc");

        final Ticket mediumTicket =
                Ticket.builder()
                        .id(101L)
                        .subject("Test")
                        .description("Test desc")
                        .status(Ticket.Status.OPEN)
                        .priority(Ticket.Priority.MEDIUM)
                        .requester(endUser)
                        .build();

        given(ticketRepository.save(any(Ticket.class))).willReturn(mediumTicket);
        given(ticketMapper.toTicketResponse(mediumTicket)).willReturn(responseFor(mediumTicket));

        final TicketResponse response = ticketService.createTicket(req, endUser);

        assertThat(response.getPriority()).isEqualTo(Ticket.Priority.MEDIUM);
    }

    @Test
    @DisplayName(
            "createTicket() with assigneeId looks up agent and passes execution to coordinator")
    void createTicket_withAssigneeId_loadsAgentAndPublishesAssignedEvent() {
        final CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Assigned ticket");
        req.setDescription("desc");
        req.setAssigneeId(1L);

        final Ticket ticketWithAssignee =
                Ticket.builder()
                        .id(102L)
                        .subject("Assigned ticket")
                        .description("desc")
                        .status(Ticket.Status.OPEN)
                        .priority(Ticket.Priority.MEDIUM)
                        .requester(endUser)
                        .assignee(agent)
                        .build();

        given(agentQuery.findOrThrow(1L)).willReturn(agent);
        given(ticketRepository.save(any(Ticket.class))).willReturn(ticketWithAssignee);
        given(ticketMapper.toTicketResponse(ticketWithAssignee))
                .willReturn(responseFor(ticketWithAssignee));

        ticketService.createTicket(req, endUser);

        then(agentQuery).should(times(1)).findOrThrow(1L);
        then(updateCoordinator).should(times(1)).handlePostCreation(ticketWithAssignee, endUser);
    }

    @Test
    @DisplayName("createTicket() throws ResourceNotFoundException for unknown assignee")
    void createTicket_unknownAssignee_throwsNotFound() {
        final CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Test");
        req.setDescription("desc");
        req.setAssigneeId(999L);

        given(agentQuery.findOrThrow(999L)).willThrow(new ResourceNotFoundException("Agent", 999L));

        assertThatThrownBy(() -> ticketService.createTicket(req, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getTicketById() returns response for authorised principal")
    void getTicketById_authorised_returnsResponse() {
        given(ticketQuery.findOrThrow(100L, endUser)).willReturn(openTicket);
        given(ticketMapper.toTicketResponse(openTicket)).willReturn(responseFor(openTicket));

        final TicketResponse response = ticketService.getTicketById(100L, endUser);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getTicketById() throws ResourceNotFoundException for unknown ID")
    void getTicketById_unknownId_throwsNotFound() {
        given(ticketQuery.findOrThrow(999L, endUser))
                .willThrow(new ResourceNotFoundException("Ticket", 999L));

        assertThatThrownBy(() -> ticketService.getTicketById(999L, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateTicket() OPEN -> IN_PROGRESS routes status change to coordinator")
    void updateTicket_openToInProgress_recordsFirstResponse() {
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.IN_PROGRESS);

        givenAuthorizedTicket(100L, openTicket, agentUser);

        // 1. Simulate the coordinator modifying the state of the ticket object
        doAnswer(
                        invocation -> {
                            Ticket t = invocation.getArgument(0);
                            t.setStatus(Ticket.Status.IN_PROGRESS);
                            return null;
                        })
                .when(updateCoordinator)
                .applyStatusChange(openTicket, Ticket.Status.IN_PROGRESS, agentUser);

        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        final TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        // 2. Verified status matches mutated state and the coordinator was called
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.IN_PROGRESS);
        then(updateCoordinator)
                .should(times(1))
                .applyStatusChange(openTicket, Ticket.Status.IN_PROGRESS, agentUser);
    }

    @Test
    @DisplayName("updateTicket() assigns agent via coordinator execution block")
    void updateTicket_assignAgent_dispatchesAssignedEvent() {
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        givenAuthorizedTicket(100L, openTicket, agentUser);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(updateCoordinator).should(times(1)).applyAssignmentChange(openTicket, 1L, agentUser);
    }

    @Test
    @DisplayName("updateTicket() skips assignment logic entirely when agent is unchanged")
    void updateTicket_sameAssignee_noEventDispatched() {
        final Ticket assigned =
                Ticket.builder()
                        .id(100L)
                        .subject("Login broken")
                        .description("desc")
                        .status(Ticket.Status.OPEN)
                        .priority(Ticket.Priority.HIGH)
                        .requester(endUser)
                        .assignee(agent)
                        .build();
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        givenAuthorizedTicket(100L, assigned, agentUser);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(updateCoordinator).should(never()).applyAssignmentChange(any(), anyLong(), any());
    }

    @Test
    @DisplayName("updateTicket() priority change triggers coordinator execution step")
    void updateTicket_priorityChange_dispatchesPriorityChanged() {
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setPriority(Ticket.Priority.URGENT);

        givenAuthorizedTicket(100L, openTicket, agentUser);

        // 1. Simulate the coordinator modifying the priority state of the ticket object
        doAnswer(
                        invocation -> {
                            Ticket t = invocation.getArgument(0);
                            t.setPriority(Ticket.Priority.URGENT);
                            return null;
                        })
                .when(updateCoordinator)
                .applyPriorityChange(openTicket, Ticket.Priority.URGENT, agentUser);

        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        final TicketResponse response = ticketService.updateTicket(100L, req, agentUser);

        // 2. Verified priority matches mutated state and the coordinator was called
        assertThat(response.getPriority()).isEqualTo(Ticket.Priority.URGENT);
        then(updateCoordinator)
                .should(times(1))
                .applyPriorityChange(openTicket, Ticket.Priority.URGENT, agentUser);
    }

    @Test
    @DisplayName("updateTicket() skips priority logic when priority is unchanged")
    void updateTicket_samePriority_noEventDispatched() {
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setPriority(Ticket.Priority.HIGH);

        givenAuthorizedTicket(100L, openTicket, agentUser);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(updateCoordinator).should(never()).applyPriorityChange(any(), any(), any());
    }

    @Test
    @DisplayName("updateTicket() escalation marks escalated status via coordinator")
    void updateTicket_escalationFromInProgress_marksEscalatedStatus() {
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setEscalated(true);
        req.setEscalationReason("SLA risk");

        givenAuthorizedTicket(100L, openTicket, agentUser);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(updateCoordinator).should(times(1)).applyEscalation(openTicket, "SLA risk", agentUser);
    }

    @Test
    @DisplayName(
            "updateTicket() escalation on an already-escalated ticket bypasses coordinator"
                    + " escalation blocks")
    void updateTicket_alreadyEscalated_noEventDispatched() {
        final Ticket alreadyEscalated =
                Ticket.builder()
                        .id(100L)
                        .subject("Login broken")
                        .description("desc")
                        .status(Ticket.Status.ESCALATED)
                        .priority(Ticket.Priority.HIGH)
                        .requester(endUser)
                        .escalated(true)
                        .build();
        final UpdateTicketRequest req = new UpdateTicketRequest();
        req.setEscalated(true);
        req.setEscalationReason("Again?");

        givenAuthorizedTicket(100L, alreadyEscalated, agentUser);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class)))
                .willAnswer(i -> responseFor(i.getArgument(0)));

        ticketService.updateTicket(100L, req, agentUser);

        then(updateCoordinator).should(never()).applyEscalation(any(), anyString(), any());
    }

    @Test
    @DisplayName("deleteTicket() dispatches deletion command via coordinator if user is admin")
    void deleteTicket_valid_dispatchesEventAndDeletes() {
        given(ticketQuery.findOrThrow(100L, adminUser)).willReturn(openTicket);

        ticketService.deleteTicket(100L, adminUser);

        then(updateCoordinator).should(times(1)).handleDeletion(openTicket, adminUser);
    }

    @Test
    @DisplayName("deleteTicket() throws AccessDeniedException when actor is non-admin user")
    void deleteTicket_nonAdminActor_throwsAccessDeniedException() {
        assertThatThrownBy(() -> ticketService.deleteTicket(100L, endUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Administrative privileges are required");

        then(ticketQuery).should(never()).findOrThrow(anyLong(), any());
        then(updateCoordinator).should(never()).handleDeletion(any(), any());
    }

    private void givenAuthorizedTicket(final Long ticketId, final Ticket ticket, final User actor) {
        given(ticketQuery.findOrThrow(ticketId, actor)).willReturn(ticket);
    }

    private TicketResponse responseFor(final Ticket ticket) {
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
