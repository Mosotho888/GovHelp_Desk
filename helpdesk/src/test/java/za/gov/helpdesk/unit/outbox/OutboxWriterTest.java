package za.gov.helpdesk.unit.outbox;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxWriter unit tests")
class OutboxWriterTest {

    @Mock private OutboxEventRepository outboxRepository;
    @Captor private ArgumentCaptor<OutboxEvent> eventCaptor;

    private OutboxWriter writer;

    @BeforeEach
    void setUp() {
        writer = new OutboxWriter(outboxRepository, new ObjectMapper());
    }

    @Test
    @DisplayName(
            "write() serialises the payload to JSON and persists a fully populated outbox event")
    void write_validPayload_persistsSerializedEvent() {
        writer.write("TICKET_EMAIL", "TICKET", 100L, Map.of("subject", "Printer broken"));

        then(outboxRepository).should(times(1)).save(eventCaptor.capture());
        final OutboxEvent saved = eventCaptor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TICKET_EMAIL");
        assertThat(saved.getAggregateType()).isEqualTo("TICKET");
        assertThat(saved.getAggregateId()).isEqualTo(100L);
        assertThat(saved.getPayload()).contains("Printer broken");
    }

    @Test
    @DisplayName("write() accepts a null aggregate id for events with no natural aggregate root")
    void write_nullAggregateId_persistsWithNullId() {
        writer.write("SYSTEM_EVENT", "SYSTEM", null, Map.of("info", "startup"));

        then(outboxRepository).should(times(1)).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAggregateId()).isNull();
    }

    @Test
    @DisplayName(
            "write() wraps a serialization failure in an IllegalStateException rather than"
                    + " propagating it raw")
    void write_unserializablePayload_wrapsInIllegalStateException() {
        // An object graph with a cycle cannot be serialized by Jackson and triggers a
        // StackOverflowError/JsonMappingException, which write() must translate.
        final CyclicPayload cyclic = new CyclicPayload();
        cyclic.self = cyclic;

        assertThatThrownBy(() -> writer.write("TICKET_EMAIL", "TICKET", 100L, cyclic))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to write outbox event");
    }

    /** Payload shape that cannot be serialised by Jackson, to exercise the failure path. */
    @SuppressWarnings("PMD.UnusedPrivateField")
    static class CyclicPayload {
        private CyclicPayload self;
    }
}
