package za.gov.helpdesk.auditlog.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.shared.RequestContextHelper;
import za.gov.helpdesk.users.model.User;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final OutboxWriter outboxWriter;
    private final RequestContextHelper requestContextHelper;

    public void publishAudit(AuditLog.EntityType entityType, Long entityId, User actor, AuditLog.AuditAction action,
            String oldValue, String newValue, String description) {

        AuditLogMessage message = AuditLogMessage.builder().entityType(entityType).entityId(entityId)
                .actorId(actor.getId()).actorName(actor.getName()).actorRole(actor.getRole().name())
                .ipAddress(requestContextHelper.getClientIp()).action(action).oldValue(oldValue).newValue(newValue)
                .description(description).isAuthLog(false).build();

        publish(message);
    }

    public void publishAuthAudit(AuditLog.AuditAction action, Long actorId, String actorName, String actorRole,
            String description) {

        AuditLogMessage message = AuditLogMessage.builder().entityType(AuditLog.EntityType.AUTH).entityId(actorId)
                .actorId(actorId).actorName(actorName).actorRole(actorRole)
                .ipAddress(requestContextHelper.getClientIp()).action(action).description(description).isAuthLog(true)
                .build();

        publish(message);
    }

    private void publish(AuditLogMessage message) {
        try {
            outboxWriter.write(OutboxEvent.EventType.AUDIT.name(), message.getEntityType().name(),
                    message.getEntityId(), message);

            log.info("Audit message queued: action={} entity={}/{}", message.getAction(), message.getEntityType(),
                    message.getEntityId());
        } catch (Exception e) {
            log.error("Failed to queue message: action={} entity={}/{} error={}", message.getAction(),
                    message.getEntityType(), message.getEntityId(), e.getMessage());
        }
    }
}
