package za.gov.helpdesk.ticket.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationPublisher;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

@Component
@RequiredArgsConstructor
public class TicketEventDispatcher {

    private final AuditEventPublisher auditPublisher;
    private final TicketEmailNotificationPublisher emailPublisher;

    public void publish(Ticket ticket, User actor, AuditLog.AuditAction action, String oldValue, String newValue,
            String description, String comment) {

        publishAudit(ticket, actor, action, oldValue, newValue, description);
        publishEmail(ticket, action, comment);
    }

    public void publishEmail(Ticket ticket, AuditLog.AuditAction action, String comment) {

        User agentUser = ticket.getAssignee() != null ? ticket.getAssignee().getUser() : null;

        emailPublisher.publish(ticket, ticket.getRequester(), agentUser, action, comment);
    }

    public void publishAudit(Ticket ticket, User actor, AuditLog.AuditAction action, String oldValue, String newValue,
            String description) {

        auditPublisher.publishAudit(AuditLog.EntityType.TICKET, ticket.getId(), actor, action, oldValue, newValue,
                description);
    }
}
