package za.gov.helpdesk.unit.services.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.mapper.AgentMapper;
import za.gov.helpdesk.agent.metrics.AgentMetrics;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.agent.service.impl.AgentServiceImpl;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentServiceImpl unit tests")
public class AgentServiceImplTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgentMapper agentMapper;
    @Mock
    private AuditEventPublisher auditPublisher;
    @Mock
    private AgentMetrics agentMetrics;

    @InjectMocks
    private AgentServiceImpl agentService;

    private User targetUser;
    private User adminUser;
    private Agent agent;

    @BeforeEach
    void setUp() {
        targetUser = User.builder().id(10L).name("New Agent").email("newagent@gov.za").role(User.Role.USER).active(true)
                .build();
        adminUser = User.builder().id(1L).name("Admin").email("admin@gov.za").role(User.Role.ADMIN).active(true)
                .build();
        agent = Agent.builder().id(5L).user(targetUser).availability(Agent.Availability.OFFLINE).build();
    }

    @Test
    @DisplayName("createAgent() promotes USER to AGENT role, saves agent, publishes audit")
    void createAgent_fromUserRole_promotesAndSavesAndAudits() {
        CreateAgentRequest req = new CreateAgentRequest();
        req.setUserId(10L);
        req.setDepartment("IT Support");

        given(userRepository.findById(10L)).willReturn(Optional.of(targetUser));
        given(agentRepository.existsByUserId(10L)).willReturn(false);
        given(agentRepository.save(any(Agent.class))).willReturn(agent);
        given(agentMapper.toAgentResponse(agent)).willReturn(agentResponseFor(agent));

        AgentResponse response = agentService.createAgent(req, adminUser);

        assertThat(response.getId()).isEqualTo(5L);

        then(agentMetrics).should(times(1)).incrementRegistered();
        // user role must be promoted before save
        then(userRepository).should(times(1))
                .save(org.mockito.ArgumentMatchers.argThat(u -> u.getRole() == User.Role.AGENT));
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.AGENT), eq(5L), eq(adminUser),
                eq(AuditLog.AuditAction.AGENT_REGISTERED), eq(null), any(), any());
    }

    @Test
    @DisplayName("createAgent() does not re-promote user already in ADMIN role")
    void createAgent_alreadyAdmin_doesNotDowngradeToAgent() {
        targetUser.setRole(User.Role.ADMIN);
        CreateAgentRequest req = new CreateAgentRequest();
        req.setUserId(10L);

        given(userRepository.findById(10L)).willReturn(Optional.of(targetUser));
        given(agentRepository.existsByUserId(10L)).willReturn(false);
        given(agentRepository.save(any(Agent.class))).willReturn(agent);
        given(agentMapper.toAgentResponse(agent)).willReturn(agentResponseFor(agent));

        agentService.createAgent(req, adminUser);

        then(agentMetrics).should(times(1)).incrementRegistered();
        then(userRepository).should(never()).save(any()); // no role change save
    }

    @Test
    @DisplayName("createAgent() throws DuplicateResourceException when user is already an agent")
    void createAgent_alreadyAgent_throwsDuplicate() {
        CreateAgentRequest req = new CreateAgentRequest();
        req.setUserId(10L);

        given(userRepository.findById(10L)).willReturn(Optional.of(targetUser));
        given(agentRepository.existsByUserId(10L)).willReturn(true);

        assertThatThrownBy(() -> agentService.createAgent(req, adminUser))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("already registered");

        then(agentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createAgent() throws ResourceNotFoundException when user does not exist")
    void createAgent_unknownUser_throwsNotFound() {
        CreateAgentRequest req = new CreateAgentRequest();
        req.setUserId(999L);

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.createAgent(req, adminUser)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateAgent() changes availability, publishes AVAILABILITY_CHANGED audit")
    void updateAgent_availabilityChange_publishesAudit() {
        UpdateAgentRequest req = new UpdateAgentRequest();
        req.setAvailability(Agent.Availability.ONLINE);

        given(agentRepository.findById(5L)).willReturn(Optional.of(agent));
        given(agentRepository.save(any(Agent.class))).willReturn(agent);
        given(agentMapper.toAgentResponse(agent)).willReturn(agentResponseFor(agent));

        agentService.updateAgent(5L, req, adminUser);

        assertThat(agent.getAvailability()).isEqualTo(Agent.Availability.ONLINE);

        then(agentMetrics).should(times(1)).incrementAvailabilityChanged();
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.AGENT), eq(5L), eq(adminUser),
                eq(AuditLog.AuditAction.AVAILABILITY_CHANGED), eq("OFFLINE"), eq("ONLINE"), eq(null));
    }

    @Test
    @DisplayName("updateAgent() skips audit when availability is unchanged")
    void updateAgent_sameAvailability_noAuditPublished() {
        UpdateAgentRequest req = new UpdateAgentRequest();
        req.setAvailability(Agent.Availability.OFFLINE); // same as current

        given(agentRepository.findById(5L)).willReturn(Optional.of(agent));
        given(agentRepository.save(any(Agent.class))).willReturn(agent);
        given(agentMapper.toAgentResponse(agent)).willReturn(agentResponseFor(agent));

        agentService.updateAgent(5L, req, adminUser);

        then(auditPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("updateAgent() changes department and publishes DEPARTMENT_CHANGED audit")
    void updateAgent_departmentChange_publishesAudit() {
        agent.setDepartment("HR");
        UpdateAgentRequest req = new UpdateAgentRequest();
        req.setDepartment("Finance");

        given(agentRepository.findById(5L)).willReturn(Optional.of(agent));
        given(agentRepository.save(any(Agent.class))).willReturn(agent);
        given(agentMapper.toAgentResponse(agent)).willReturn(agentResponseFor(agent));

        agentService.updateAgent(5L, req, adminUser);

        assertThat(agent.getDepartment()).isEqualTo("Finance");

        then(agentMetrics).should(times(1)).incrementDepartmentChanged();
        then(auditPublisher).should(times(1)).publishAudit(eq(AuditLog.EntityType.AGENT), eq(5L), eq(adminUser),
                eq(AuditLog.AuditAction.DEPARTMENT_CHANGED), eq("HR"), eq("Finance"), eq(null));
    }

    @Test
    @DisplayName("updateAgent() throws ResourceNotFoundException for unknown agent")
    void updateAgent_unknownAgent_throwsNotFound() {
        given(agentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.updateAgent(999L, new UpdateAgentRequest(), adminUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAgentById() throws ResourceNotFoundException for unknown agent")
    void getAgentById_unknownId_throwsNotFound() {
        given(agentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.getAgentById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private AgentResponse agentResponseFor(Agent a) {
        return AgentResponse.builder().id(a.getId()).availability(a.getAvailability()).department(a.getDepartment())
                .build();
    }
}
