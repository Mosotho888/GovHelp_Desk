package za.gov.helpdesk.notification.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaEmailNotificationPublisher {

    private final OutboxWriter outboxWriter;

    public void publishWarning(String agentEmail,
                               String agentName,
                               String ticketNumber,
                               Long ticketId,
                               String ticketSubject,
                               String deadlineType,
                               LocalDateTime dueAt) {

        SlaEmailNotificationMessage message = SlaEmailNotificationMessage.builder()
                .agentEmail(agentEmail)
                .agentName(agentName)
                .ticketId(ticketId)
                .ticketNumber(ticketNumber)
                .ticketSubject(ticketSubject)
                .deadlineType(deadlineType)
                .dueAt(dueAt)
                .isWarning(true)
                .build();

        publish(message);

    }

    public void publishBreach(String agentEmail,
                              String agentName,
                              String ticketNumber,
                              Long ticketId,
                              String ticketSubject,
                              String deadlineType) {

        SlaEmailNotificationMessage message = SlaEmailNotificationMessage.builder()
                .agentEmail(agentEmail)
                .agentName(agentName)
                .ticketId(ticketId)
                .ticketNumber(ticketNumber)
                .ticketSubject(ticketSubject)
                .deadlineType(deadlineType)
                .isWarning(false)
                .build();

        publish(message);

    }

    public void publish(SlaEmailNotificationMessage message) {

        outboxWriter.write(
                OutboxEvent.EventType.SLA_EMAIL.name(),
                AuditLog.EntityType.TICKET.name(),
                message.getTicketId(),
                message
        );
    }
}
