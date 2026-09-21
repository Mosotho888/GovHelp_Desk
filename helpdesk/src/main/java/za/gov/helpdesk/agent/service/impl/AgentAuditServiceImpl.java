package za.gov.helpdesk.agent.service.impl;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.service.AgentAuditService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentAuditServiceImpl implements AgentAuditService {

    private final AuditEventPublisher auditPublisher;

    @Override
    public void agentCreated(final Agent savedAgent, final User actor, final User user) {
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
    }

    @Override
    public void agentUpdatedAvailability(
            final Agent agent, final User actor, final String newAvailability) {
        auditPublisher.publishAudit(
                AuditLog.EntityType.AGENT,
                agent.getId(),
                actor,
                AuditLog.AuditAction.AVAILABILITY_CHANGED,
                agent.getAvailability().name(),
                newAvailability,
                null);
    }

    @Override
    public void agentUpdatedDepartment(
            final Agent agent, final User actor, final String newDepartment) {
        auditPublisher.publishAudit(
                AuditLog.EntityType.AGENT,
                agent.getId(),
                actor,
                AuditLog.AuditAction.DEPARTMENT_CHANGED,
                agent.getDepartment(),
                newDepartment,
                null);
    }
}
