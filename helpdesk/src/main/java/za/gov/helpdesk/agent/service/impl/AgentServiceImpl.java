package za.gov.helpdesk.agent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.agent.mapper.AgentMapper;
import za.gov.helpdesk.agent.metrics.AgentMetrics;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.agent.repository.jdbc.ReportJdbcRepository;
import za.gov.helpdesk.agent.service.AgentService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final AgentMapper agentMapper;
    private final ReportJdbcRepository reportJdbcRepository;
    private final AuditEventPublisher auditPublisher;
    private final AgentMetrics agentMetrics;

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request, User actor) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        if (agentRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException("User " +  request.getUserId() + " is already registered as an agent");
        }

        // Promote user role to AGENT if not already ADMIN
        if (user.getRole() == User.Role.USER) {
            user.setRole(User.Role.AGENT);
            userRepository.save(user);
        }

        Agent agent = Agent.builder()
                .user(user)
                .department(request.getDepartment())
                .availability(request.getAvailability() != null ? request.getAvailability() : Agent.Availability.OFFLINE)
                .build();

        Agent savedAgent = agentRepository.save(agent);
        agentMetrics.incrementRegistered();

        auditPublisher.publishAudit(
                AuditLog.EntityType.AGENT,
                savedAgent.getId(),
                actor,
                AuditLog.AuditAction.AGENT_REGISTERED,
                null,
                user.getName(),
                "Registered as agent in department: " + (savedAgent.getDepartment() != null ? savedAgent.getDepartment() : "N/A")
        );

        return agentMapper.toAgentResponse(savedAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(Long id) {

        return agentMapper.toAgentResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentResponse> getAllAgents(Pageable pageable) {

        return agentRepository.findAll(pageable).map(agentMapper::toAgentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentStatsResponse getAgentStats(Long id) {
        Agent agent = findOrThrow(id);
        Map<String, Object> raw = reportJdbcRepository.getAgentsStats(id);

        return AgentStatsResponse.builder()
                .agentId(id)
                .agentName(agent.getUser().getName())
                .totalAssigned(toLong(raw.get("total_assigned")))
                .openCount(toLong(raw.get("open_count")))
                .inProgressCount(toLong(raw.get("in_progress_count")))
                .resolvedCount(toLong(raw.get("resolved_count")))
                .closedCount(toLong(raw.get("closed_count")))
                .escalatedCount(toLong(raw.get("escalated_count")))
                .avgResolutionHours(toDouble(raw.get("avg_resolution_hours")))
                .build();
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(Long id, UpdateAgentRequest request, User actor) {
        Agent agent = findOrThrow(id);

        if (request.getAvailability() != null && !request.getAvailability().equals(agent.getAvailability())) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.AGENT,
                    agent.getId(),
                    actor,
                    AuditLog.AuditAction.AVAILABILITY_CHANGED,
                    agent.getAvailability().name(),
                    request.getAvailability().name(),
                    null
            );
            agent.setAvailability(request.getAvailability());
            agentMetrics.incrementAvailabilityChanged();
        }

        if (request.getDepartment() != null && !request.getDepartment().equals(agent.getDepartment())) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.AGENT,
                    agent.getId(),
                    actor,
                    AuditLog.AuditAction.DEPARTMENT_CHANGED,
                    agent.getDepartment(),
                    request.getDepartment(),
                    null
            );
            agent.setDepartment(request.getDepartment());
            agentMetrics.incrementDepartmentChanged();
        }
        return agentMapper.toAgentResponse(agentRepository.save(agent));
    }

    private Agent findOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        return ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }
}
