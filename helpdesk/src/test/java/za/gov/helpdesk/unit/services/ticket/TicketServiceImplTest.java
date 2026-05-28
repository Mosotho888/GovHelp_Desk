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
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationPublisher;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.mapper.TicketMapper;
import za.gov.helpdesk.ticket.model.Ticket;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
    private AuditEventPublisher auditPublisher;
    @Mock
    private TicketEmailNotificationPublisher emailPublisher;
    @Mock
    private SlaService slaService;

    @InjectMocks
    private TicketServiceImpl ticketServiceImpl;

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
    @DisplayName("createTicket() persists ticket and initializes SLA")
    void createTicket_validRequest_savesInitializesSlaAndPublishesNotifications() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Login broken");
        req.setDescription("Cannot access dashboard");
        req.setPriority(Ticket.Priority.HIGH);

        given(ticketRepository.save(any(Ticket.class))).willReturn(openTicket);
        given(ticketMapper.toTicketResponse(openTicket)).willReturn(responseFor(openTicket));

        TicketResponse response = ticketServiceImpl.createTicket(req, endUser);

        assertThat(response.getSubject()).isEqualTo("Login broken");
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.OPEN);
        then(ticketRepository).should(times(1)).save(any(Ticket.class));
        then(slaService).should(times(1)).initializeSla(openTicket);
        then(emailPublisher).should(times(1)).publish(
                eq(openTicket),
                eq(endUser),
                eq(null),
                eq(AuditLog.AuditAction.TICKET_CREATED),
                eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() OPEN -> IN_PROGRESS records first response")
    void updateTicket_openToInProgress_recordsFirstResponse() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.IN_PROGRESS);

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketServiceImpl.updateTicket(100L, req, agentUser);

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.IN_PROGRESS);
        then(slaService).should(times(1)).recordFirstResponse(100L);
        then(slaService).should(never()).recordResolution(anyLong());
        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.TICKET),
                eq(100L),
                eq(agentUser),
                eq(AuditLog.AuditAction.STATUS_CHANGED),
                eq("OPEN"),
                eq("IN_PROGRESS"),
                eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() IN_PROGRESS -> RESOLVED records resolution")
    void updateTicket_inProgressToResolved_recordsResolution() {
        Ticket inProgressTicket = Ticket.builder()
                .id(100L)
                .subject("Login broken")
                .description("Cannot access dashboard")
                .status(Ticket.Status.IN_PROGRESS)
                .priority(Ticket.Priority.HIGH)
                .requester(endUser)
                .assignee(agent)
                .build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.RESOLVED);

        givenAuthorizedTicket(100L, agentUser, inProgressTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketServiceImpl.updateTicket(100L, req, agentUser);

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.RESOLVED);
        then(slaService).should(times(1)).recordResolution(100L);
        then(slaService).should(never()).recordFirstResponse(anyLong());
    }

    @Test
    @DisplayName("updateTicket() RESOLVED -> CLOSED publishes close notification")
    void updateTicket_resolvedToClosed_publishesTicketClosed() {
        Ticket resolvedTicket = Ticket.builder()
                .id(100L)
                .subject("Login broken")
                .description("Cannot access dashboard")
                .status(Ticket.Status.RESOLVED)
                .priority(Ticket.Priority.HIGH)
                .requester(endUser)
                .assignee(agent)
                .build();
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.CLOSED);

        givenAuthorizedTicket(100L, agentUser, resolvedTicket);
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        TicketResponse response = ticketServiceImpl.updateTicket(100L, req, agentUser);

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.CLOSED);
        then(emailPublisher).should(times(1)).publish(
                eq(resolvedTicket),
                eq(endUser),
                eq(agentUser),
                eq(AuditLog.AuditAction.TICKET_CLOSED),
                eq(null)
        );
    }

    @Test
    @DisplayName("updateTicket() CLOSED -> OPEN throws InvalidStatusTransitionException")
    void updateTicket_invalidTransition_throwsException() {
        Ticket closedTicket = Ticket.builder()
                .id(200L)
                .subject("Old issue")
                .description("Resolved")
                .status(Ticket.Status.CLOSED)
                .requester(endUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.OPEN);

        givenAuthorizedTicket(200L, agentUser, closedTicket);

        assertThatThrownBy(() -> ticketServiceImpl.updateTicket(200L, req, agentUser))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("OPEN");

        then(ticketRepository).should(never()).save(any());
        then(auditPublisher).should(never()).publishAudit(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("getTicketById() throws ResourceNotFoundException for unknown ID")
    void getTicketById_unknownId_throwsNotFound() {
        given(ticketRepository.findByIdAndPrincipal(999L, endUser.getEmail(), endUser.getRole().name()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketServiceImpl.getTicketById(999L, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateTicket() assigns agent and publishes ASSIGNED_TO_AGENT audit event")
    void updateTicket_assignAgent_publishesAuditEvent() {
        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        givenAuthorizedTicket(100L, agentUser, openTicket);
        given(agentRepository.findById(1L)).willReturn(Optional.of(agent));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));
        given(ticketMapper.toTicketResponse(any(Ticket.class))).willAnswer(i -> responseFor(i.getArgument(0)));

        ticketServiceImpl.updateTicket(100L, req, agentUser);

        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.TICKET),
                eq(100L),
                eq(agentUser),
                eq(AuditLog.AuditAction.ASSIGNED_TO_AGENT),
                eq("Unassigned"),
                eq(agentUser.getName()),
                eq(null)
        );
        then(emailPublisher).should(times(1)).publish(
                eq(openTicket),
                eq(endUser),
                eq(agentUser),
                eq(AuditLog.AuditAction.ASSIGNED_TO_AGENT),
                eq(null)
        );
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
