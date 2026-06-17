package za.gov.helpdesk.outbox.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWriter {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void write(String eventType, String aggregateType, Long aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            outboxRepository.save(OutboxEvent.builder().eventType(eventType).aggregateType(aggregateType)
                    .aggregateId(aggregateId).payload(json).build());

            log.debug("Outbox event queued: type={} aggregate={}/{}", eventType, aggregateType, aggregateId);

        } catch (Exception e) {
            // Rethrow so the enclosing business transaction rolls back —
            // if we cannot record the event we must not silently commit
            throw new IllegalStateException("Failed to write outbox event: type=" + eventType, e);
        }
    }
}
