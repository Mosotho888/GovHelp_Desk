package za.gov.helpdesk.unit.services.agent;

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
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.agent.service.impl.AgentRoleLifecycleServiceImpl;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentRoleLifecycleServiceImpl unit tests")
class AgentRoleLifecycleServiceImplTest {

    @Mock private AgentRepository agentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private AuditEventPublisher auditPublisher;
    @Captor private ArgumentCaptor<Agent> agentCaptor;

    private AgentRoleLifecycleServiceImpl service;

    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        service =
                new AgentRoleLifecycleServiceImpl(
                        agentRepository, ticketRepository, auditPublisher);
        admin =
                User.builder()
                        .id(1L)
                        .name("Admin User")
                        .email("admin@gov.za")
                        .role(Role.ADMIN)
                        .build();
        target = User.builder().id(2L).name("New Agent").email("target@gov.za").build();
    }

    // ---- promotion ----

    @Test
    @DisplayName("handleRoleChange() to AGENT creates a new agent profile when none exists")
    void handleRoleChange_promoteNoExistingProfile_createsNewAgentProfile() {
        given(agentRepository.existsByUserId(2L)).willReturn(false);

        service.handleRoleChange(target, Role.USER, Role.AGENT, admin);

        then(agentRepository).should(times(1)).save(agentCaptor.capture());
        final Agent saved = agentCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(target);
        assertThat(saved.getAvailability()).isEqualTo(Agent.Availability.OFFLINE);
        then(auditPublisher)
                .should(times(1))
                .publishAudit(
                        eq(AuditLog.EntityType.AGENT),
                        eq(2L),
                        eq(admin),
                        eq(AuditLog.AuditAction.AGENT_REGISTERED),
                        isNull(),
                        eq("target@gov.za"),
                        any());
    }

    @Test
    @DisplayName(
            "handleRoleChange() to AGENT reactivates a dormant existing agent profile without a new"
                    + " audit entry")
    void handleRoleChange_promoteExistingProfile_reactivatesWithoutAudit() {
        final Agent dormantAgent =
                Agent.builder()
                        .id(9L)
                        .user(target)
                        .availability(Agent.Availability.OFFLINE)
                        .build();
        given(agentRepository.existsByUserId(2L)).willReturn(true);
        given(agentRepository.findByUserId(2L)).willReturn(Optional.of(dormantAgent));

        service.handleRoleChange(target, Role.USER, Role.AGENT, admin);

        then(agentRepository).should(times(1)).save(dormantAgent);
        assertThat(dormantAgent.getAvailability()).isEqualTo(Agent.Availability.OFFLINE);
        then(auditPublisher)
                .should(never())
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }

    // ---- demotion ----

    @Test
    @DisplayName(
            "handleRoleChange() away from AGENT sets the agent OFFLINE and unassigns their tickets")
    void handleRoleChange_demoteFromAgent_setsOfflineAndUnassignsTickets() {
        final Agent existingAgent =
                Agent.builder().id(9L).user(target).availability(Agent.Availability.ONLINE).build();
        given(agentRepository.findByUserId(2L)).willReturn(Optional.of(existingAgent));
        given(ticketRepository.unassignFromAgent(existingAgent)).willReturn(3);

        service.handleRoleChange(target, Role.AGENT, Role.USER, admin);

        assertThat(existingAgent.getAvailability()).isEqualTo(Agent.Availability.OFFLINE);
        then(agentRepository).should(times(1)).save(existingAgent);
        then(auditPublisher)
                .should(times(1))
                .publishAudit(
                        eq(AuditLog.EntityType.AGENT),
                        eq(2L),
                        eq(admin),
                        eq(AuditLog.AuditAction.AVAILABILITY_CHANGED),
                        eq("ONLINE"),
                        eq("OFFLINE"),
                        any());
    }

    @Test
    @DisplayName("handleRoleChange() demotion is a no-op when the user has no agent profile")
    void handleRoleChange_demoteNoAgentProfile_doesNothing() {
        given(agentRepository.findByUserId(2L)).willReturn(Optional.empty());

        service.handleRoleChange(target, Role.AGENT, Role.USER, admin);

        then(agentRepository).should(never()).save(any());
        then(ticketRepository).should(never()).unassignFromAgent(any());
    }

    @Test
    @DisplayName("handleRoleChange() between two non-agent roles does nothing at all")
    void handleRoleChange_neitherRoleIsAgent_doesNothing() {
        service.handleRoleChange(target, Role.USER, Role.ADMIN, admin);

        then(agentRepository).should(never()).save(any());
        then(agentRepository).should(never()).findByUserId(any());
        then(auditPublisher)
                .should(never())
                .publishAudit(any(), any(), any(), any(), any(), any(), any());
    }
}
