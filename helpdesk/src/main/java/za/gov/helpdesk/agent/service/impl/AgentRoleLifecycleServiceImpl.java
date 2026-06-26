package za.gov.helpdesk.agent.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.agent.service.AgentRoleLifecycleService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRoleLifecycleServiceImpl implements AgentRoleLifecycleService {

    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public void handleRoleChange(
            final User target, final User.Role oldRole, final User.Role newRole, final User admin) {
        if (newRole == User.Role.AGENT) {
            handlePromotion(target, oldRole, admin);
        } else if (oldRole == User.Role.AGENT) {
            handleDemotion(target, admin);
        }
    }

    private void handlePromotion(final User target, final User.Role oldRole, final User admin) {
        if (agentRepository.existsByUserId(target.getId())) {

            agentRepository
                    .findByUserId(target.getId())
                    .ifPresent(
                            agent -> {
                                agent.setAvailability(Agent.Availability.OFFLINE);
                                agentRepository.save(agent);
                            });
            log.info("Existing agent profile reactivated for user={}", target.getEmail());
            return;
        }

        final Agent agent =
                Agent.builder().user(target).availability(Agent.Availability.OFFLINE).build();
        agentRepository.save(agent);

        auditPublisher.publishAudit(
                AuditLog.EntityType.AGENT,
                target.getId(),
                admin,
                AuditLog.AuditAction.AGENT_REGISTERED,
                null,
                target.getEmail(),
                "Agent profile created on promotion from "
                        + oldRole.name()
                        + " by admin: "
                        + admin.getEmail());

        log.info(
                "Agent profile created for user={} by admin={}",
                target.getEmail(),
                admin.getEmail());
    }

    private void handleDemotion(final User target, final User admin) {
        agentRepository
                .findByUserId(target.getId())
                .ifPresent(
                        agent -> {
                            final Agent.Availability previousAvailability = agent.getAvailability();

                            agent.setAvailability(Agent.Availability.OFFLINE);
                            agentRepository.save(agent);

                            final int unassigned = ticketRepository.unassignFromAgent(agent);

                            auditPublisher.publishAudit(
                                    AuditLog.EntityType.AGENT,
                                    target.getId(),
                                    admin,
                                    AuditLog.AuditAction.AVAILABILITY_CHANGED,
                                    previousAvailability.name(),
                                    Agent.Availability.OFFLINE.name(),
                                    "Agent set OFFLINE on demotion. "
                                            + unassigned
                                            + " ticket(s) unassigned. Admin: "
                                            + admin.getEmail());

                            log.info(
                                    "Agent demoted: user={} {} ticket(s) unassigned by admin={}",
                                    target.getEmail(),
                                    unassigned,
                                    admin.getEmail());
                        });
    }
}
