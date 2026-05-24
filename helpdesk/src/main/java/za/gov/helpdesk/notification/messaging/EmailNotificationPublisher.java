package za.gov.helpdesk.notification.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.EmailNotificationMessage;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Ticket ticket,
                        User customer,
                        User agentUser,
                        AuditLog.AuditAction trigger,
                        String comment) {

        EmailNotificationMessage message = EmailNotificationMessage.builder()
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
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE,
                    RabbitMQConstants.EMAIL_ROUTING_KEY,
                    message
            );
            log.info("Email notification queued: trigger={} ticket={}",
                    trigger, ticket.getId());
        } catch (Exception e) {
            // Broker down — log but don't fail the ticket operation
            log.error("Failed to queue email notification: trigger={} ticket={} error={}",
                    trigger, ticket.getId(), e.getMessage());
        }
    }
}
