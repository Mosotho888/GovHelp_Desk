package za.gov.helpdesk.unit.services.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.dto.request.CreateTicketRequest;
import za.gov.helpdesk.ticket.dto.request.UpdateTicketRequest;
import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.ticket.service.impl.TicketServiceImpl;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImplTest unit tests")
public class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditEventPublisher auditPublisher;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

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
                .id(100L).subject("Login broken").description("Cannot access dashboard")
                .status(Ticket.Status.OPEN).priority(Ticket.Priority.HIGH)
                .requester(endUser).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createTicket() persists ticket and publishes audit event")
    void createTicket_validRequest_savesAndPublishesAudit() {

        mockAuthenticatedUser();

        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Login broken");
        req.setDescription("Cannot access dashboard");
        req.setPriority(Ticket.Priority.HIGH);

        given(authentication.getName()).willReturn("john@citizen.za");
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(endUser));
        given(ticketRepository.save(any(Ticket.class))).willReturn(openTicket);

        TicketResponse response = ticketServiceImpl.createTicket(req, endUser);

        assertThat(response.getSubject()).isEqualTo("Login broken");
        assertThat(response.getStatus()).isEqualTo(Ticket.Status.OPEN);
        then(ticketRepository).should(times(1)).save(any(Ticket.class));

        then(auditPublisher).should(times(1)).publishAudit(
                eq(AuditLog.EntityType.TICKET),
                eq(openTicket.getId()),
                eq(endUser),
                eq(AuditLog.AuditAction.TICKET_CREATED),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("updateTicket() OPEN -> IN_PROGRESS publishes STATUS_CHANGED and audit event")
    void updateTicket_validTransition_publishesAuditEvent() {

        mockAuthenticatedUser();

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.IN_PROGRESS);

        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(agentUser));
        given(ticketRepository.findById(100L)).willReturn(Optional.of(openTicket));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));

        TicketResponse response = ticketServiceImpl.updateTicket(100L, req, agentUser);

        assertThat(response.getStatus()).isEqualTo(Ticket.Status.IN_PROGRESS);

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
    @DisplayName("updateTicket() CLOSED -> OPEN throws InvalidStatusTransitionException")
    void updateTicket_invalidTransition_throwsException() {

        mockAuthenticatedUser();

        Ticket closedTicket = Ticket.builder()
                .id(200L).subject("Old issue").description("Resolved")
                .status(Ticket.Status.CLOSED).requester(endUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setStatus(Ticket.Status.OPEN);

        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(agentUser));
        given(ticketRepository.findById(200L)).willReturn(Optional.of(closedTicket));

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
        given(ticketRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketServiceImpl.getTicketById(999L, endUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateTicket() assigns agent and publishes ASSIGNED_TO_AGENT audit event")
    void updateTicket_assignAgent_publishesAuditEvent() {

        mockAuthenticatedUser();

        UpdateTicketRequest req = new UpdateTicketRequest();
        req.setAssigneeId(1L);

        given(authentication.getName()).willReturn("jane@gov.za");
        given(userRepository.findByEmail("jane@gov.za")).willReturn(Optional.of(agentUser));
        given(ticketRepository.findById(100L)).willReturn(Optional.of(openTicket));
        given(agentRepository.findById(1L)).willReturn(Optional.of(agent));
        given(ticketRepository.save(any(Ticket.class))).willAnswer(i -> i.getArgument(0));

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
    }

    private void mockAuthenticatedUser() {
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
    }
}
