package za.gov.helpdesk.outbox.relay;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service component responsible for persisting outbound domain events into the datastore.
 * Serializes event payloads into stringified JSON and hooks directly into active database
 * transactional context boundaries to support the atomicity goals of the Transactional Outbox
 * Pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWriter {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Serializes a structured data object and saves it into the outbox event repository. This
     * method runs inside an inherited or newly initialized database transaction boundary. If
     * serialization fails or the repository cannot persist the log, a runtime exception is thrown
     * to force a rollback of the accompanying business transaction, preventing data inconsistency.
     *
     * @param eventType the specific domain event categorization signature (e.g., "TICKET_EMAIL")
     * @param aggregateType the entity classification name serving as the aggregate root (e.g.,
     *     "TICKET")
     * @param aggregateId the primary unique database identifier tracking the aggregate root
     *     instance, or null if unmapped
     * @param payload the data transfer object containing state properties to transform into an
     *     inline JSON string
     * @throws IllegalStateException if an error occurs during JSON marshalling or database state
     *     synchronization
     */
    @Transactional
    public void write(
            final String eventType,
            final String aggregateType,
            final Long aggregateId,
            final Object payload) {
        try {
            final String json = objectMapper.writeValueAsString(payload);

            outboxRepository.save(
                    OutboxEvent.builder()
                            .eventType(eventType)
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .payload(json)
                            .build());

            log.debug(
                    "Outbox event queued: type={} aggregate={}/{}",
                    eventType,
                    aggregateType,
                    aggregateId);

        } catch (final Exception e) {
            // Rethrow so the enclosing business transaction rolls back —
            // if we cannot record the event we must not silently commit
            throw new IllegalStateException("Failed to write outbox event: type=" + eventType, e);
        }
    }
}
