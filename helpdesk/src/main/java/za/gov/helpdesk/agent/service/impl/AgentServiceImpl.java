package za.gov.helpdesk.agent.service.impl;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.dto.request.CreateAgentRequest;
import za.gov.helpdesk.agent.dto.request.UpdateAgentRequest;
import za.gov.helpdesk.agent.dto.response.AgentResponse;
import za.gov.helpdesk.agent.dto.response.AgentStatsResponse;
import za.gov.helpdesk.agent.mapper.AgentMapper;
import za.gov.helpdesk.agent.metrics.AgentMetrics;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jdbc.ReportJdbcRepository;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.agent.service.AgentQueryHelper;
import za.gov.helpdesk.agent.service.AgentService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.service.UserQueryHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final UserQueryHelper userQuery;
    private final AgentQueryHelper agentQuery;
    private final AgentMapper agentMapper;
    private final ReportJdbcRepository reportJdbcRepository;
    private final AuditEventPublisher auditPublisher;
    private final AgentMetrics agentMetrics;

    @Override
    @Transactional
    public AgentResponse createAgent(final CreateAgentRequest request, final User actor) {

        final User user = userQuery.findAndPromoteToAgentOrThrow(request.getUserId());

        if (agentRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException(
                    "User " + request.getUserId() + " is already registered as an agent");
        }

        final Agent agent =
                Agent.builder()
                        .user(user)
                        .department(request.getDepartment())
                        .availability(
                                request.getAvailability() != null
                                        ? request.getAvailability()
                                        : Agent.Availability.OFFLINE)
                        .build();

        final Agent savedAgent;

        try {
            savedAgent = agentRepository.save(agent);
        } catch (final DataIntegrityViolationException ex) {
            log.error(
                    "Conflict: User {} simultaneously assigned as agent", request.getUserId(), ex);
            throw new DuplicateResourceException(
                    "User " + request.getUserId() + " is already registered as an agent", ex);
        }
        agentMetrics.incrementRegistered();

        auditPublisher.publishAudit(
                AuditLog.EntityType.AGENT,
                savedAgent.getId(),
                actor,
                AuditLog.AuditAction.AGENT_REGISTERED,
                null,
                user.getName(),
                "Registered as agent in department: "
                        + (savedAgent.getDepartment() != null
                                ? savedAgent.getDepartment()
                                : "N/A"));

        return agentMapper.toAgentResponse(savedAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(final Long id) {

        return agentMapper.toAgentResponse(agentQuery.findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentResponse> getAllAgents(final Pageable pageable) {

        return agentRepository.findAll(pageable).map(agentMapper::toAgentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentStatsResponse getAgentStats(final Long id) {
        final Agent agent = agentQuery.findOrThrow(id);
        final Map<String, Object> raw = reportJdbcRepository.getAgentsStats(id);

        return agentMapper.toAgentStatsResponse(id, agent.getUser().getName(), raw);
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(
            final Long id, final UpdateAgentRequest request, final User actor) {
        final Agent agent = agentQuery.findAndValidateAccess(id, actor);

        if (request.getAvailability() != null
                && request.getAvailability() != agent.getAvailability()) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.AGENT,
                    agent.getId(),
                    actor,
                    AuditLog.AuditAction.AVAILABILITY_CHANGED,
                    agent.getAvailability().name(),
                    request.getAvailability().name(),
                    null);
            agent.setAvailability(request.getAvailability());
            agentMetrics.incrementAvailabilityChanged();
        }

        if (request.getDepartment() != null
                && !request.getDepartment().equals(agent.getDepartment())) {

            auditPublisher.publishAudit(
                    AuditLog.EntityType.AGENT,
                    agent.getId(),
                    actor,
                    AuditLog.AuditAction.DEPARTMENT_CHANGED,
                    agent.getDepartment(),
                    request.getDepartment(),
                    null);
            agent.setDepartment(request.getDepartment());
            agentMetrics.incrementDepartmentChanged();
        }
        return agentMapper.toAgentResponse(agentRepository.save(agent));
    }
}
