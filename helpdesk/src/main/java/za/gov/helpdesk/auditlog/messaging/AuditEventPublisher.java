package za.gov.helpdesk.auditlog.messaging;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.auditlog.dto.messaging.AuditLogMessage;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.shared.RequestContextHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message producer responsible for preparing and staging asynchronous audit trail events.
 * Constructs audit payloads from operational or security contexts and commits them safely via the
 * {@link OutboxWriter} to guarantee reliable delivery using the Transactional Outbox Pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private final OutboxWriter outboxWriter;
    private final RequestContextHelper requestContextHelper;

    /**
     * Stages an operational audit log message for entity mutations and modifications. Enriches the
     * event payload with the remote HTTP client IP address resolved from the request context.
     *
     * @param entityType the domain class model structure affected by the event
     * @param entityId the database unique primary identifier of the mutated entity record
     * @param actor the authenticated {@link User} entity executing the transactional mutation
     * @param action the category of state transition being executed (e.g., CREATE, UPDATE)
     * @param oldValue a serialized snapshots of historical properties prior to state adjustment
     *     (nullable)
     * @param newValue a serialized snapshot of adjusted properties following state adjustment
     *     (nullable)
     * @param description a human-readable summary detailing why the system transaction occurred
     */
    public void publishAudit(
            final AuditLog.EntityType entityType,
            final Long entityId,
            final User actor,
            final AuditLog.AuditAction action,
            final String oldValue,
            final String newValue,
            final String description) {

        final AuditLogMessage message =
                AuditLogMessage.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .actorId(actor.getId())
                        .actorName(actor.getName())
                        .actorRole(actor.getRole().name())
                        .ipAddress(requestContextHelper.getClientIp())
                        .action(action)
                        .oldValue(oldValue)
                        .newValue(newValue)
                        .description(description)
                        .isAuthLog(false)
                        .build();

        publish(message);
    }

    /**
     * Stages a dedicated security auditing payload to log authentication and authorization events.
     * Tracks identity perimeter boundaries such as successful token generation or sign-in failures.
     *
     * @param action the security checkpoint state transition being executed (e.g., LOGIN_SUCCESS)
     * @param actorId the unique identifier of the security principal context involved
     * @param actorName the alphanumeric handle or identification login name of the target principal
     * @param actorRole the string authority permission layer assigned to the principal execution
     *     scope
     * @param description a contextual log message summarizing the access boundary assessment
     *     outcome
     */
    public void publishAuthAudit(
            final AuditLog.AuditAction action,
            final Long actorId,
            final String actorName,
            final String actorRole,
            final String description) {

        final AuditLogMessage message =
                AuditLogMessage.builder()
                        .entityType(AuditLog.EntityType.AUTH)
                        .entityId(actorId)
                        .actorId(actorId)
                        .actorName(actorName)
                        .actorRole(actorRole)
                        .ipAddress(requestContextHelper.getClientIp())
                        .action(action)
                        .description(description)
                        .isAuthLog(true)
                        .build();

        publish(message);
    }

    /**
     * Serializes the transaction event data and writes it into the Outbox repository engine. This
     * ensures atomicity between business operations and event emission.
     *
     * @param message the structured audit log payload data record to publish
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void publish(final AuditLogMessage message) {
        try {
            outboxWriter.write(
                    OutboxEvent.EventType.AUDIT.name(),
                    message.getEntityType().name(),
                    message.getEntityId(),
                    message);

            log.info(
                    "Audit message queued: action={} entity={}/{}",
                    message.getAction(),
                    message.getEntityType(),
                    message.getEntityId());
        } catch (final Exception e) {
            log.error(
                    "Failed to write transaction record into local outbox queue engine: action={}"
                            + " entity={}/{}",
                    message.getAction(),
                    message.getEntityType(),
                    message.getEntityId(),
                    e);
        }
    }
}
