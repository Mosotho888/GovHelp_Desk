package za.gov.helpdesk.notification.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.EntityType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    public void publish(Ticket ticket,
                        User customer,
                        User agentUser,
                        AuditLog.AuditAction trigger,
                        String comment) {

        TicketEmailNotificationMessage message = TicketEmailNotificationMessage.builder()
                .trigger(trigger)
                .ticketId(ticket.getId())
                .ticketNumber("TKT-" + ticket.getId())
                .ticketSubject(ticket.getSubject())
                .ticketStatus(ticket.getStatus().name())
                .ticketPriority(ticket.getPriority().name())
                .comment(comment)
                .customerEmail(customer.getEmail())
                .customerName(customer.getName())
                .agentEmail(agentUser != null ? agentUser.getEmail() : null)
                .agentName(agentUser  != null ? agentUser.getName()  : null)
                .build();

        try {

            outboxWriter.write(
                    OutboxEvent.EventType.TICKET_EMAIL.name(),
                    AuditLog.EntityType.TICKET.name(),
                    message.getTicketId(),
                    message
            );

            log.info("Email notification queued: trigger={} ticket={}",
                    trigger, ticket.getId());
        } catch (Exception e) {
            // Broker down - log but don't fail the ticket operation
            log.error("Failed to queue email notification: trigger={} ticket={} error={}",
                    trigger, ticket.getId(), e.getMessage());
        }
    }
}
