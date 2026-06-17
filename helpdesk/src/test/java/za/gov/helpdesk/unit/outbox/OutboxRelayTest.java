package za.gov.helpdesk.unit.outbox;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.metrics.OutboxMetrics;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxRelay;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay unit tests")
public class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private OutboxMetrics outboxMetrics;

    @InjectMocks
    private OutboxRelay relay;

    // Real ObjectMapper so JSON round-trip works
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectMapper() throws Exception {
        relay = new OutboxRelay(outboxRepository, rabbitTemplate, objectMapper, outboxMetrics);
    }

    @Test
    @DisplayName("processOne() deserialises payload, publishes to correct routing key, marks PROCESSED")
    void processOne_validTicketEmail_publishesAndMarksProcessed() throws Exception {
        TicketEmailNotificationMessage msg = TicketEmailNotificationMessage.builder().ticketId(100L)
                .ticketNumber("TKT-100").customerEmail("john@citizen.za").build();
        String payload = objectMapper.writeValueAsString(msg);

        OutboxEvent event = pendingEvent("TICKET_EMAIL", payload);
        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        relay.processOne(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();

        then(outboxMetrics).should(times(1)).incrementPublished();
        then(rabbitTemplate).should(times(1)).convertAndSend(eq(RabbitMQConstants.EXCHANGE),
                eq(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY), any(TicketEmailNotificationMessage.class));
    }

    @Test
    @DisplayName("processOne() skips events that are no longer PENDING (race-condition guard)")
    void processOne_noLongerPending_skipsPublish() {
        OutboxEvent processing = pendingEvent("TICKET_EMAIL", "{}");
        processing.setStatus(OutboxEvent.Status.PROCESSING);
        given(outboxRepository.findById(processing.getId())).willReturn(Optional.of(processing));

        relay.processOne(processing);

        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processOne() marks event FAILED after max retries (5 attempts)")
    void processOne_maxAttemptsReached_marksFailed() throws Exception {
        OutboxEvent event = pendingEvent("TICKET_EMAIL", "{bad json}");
        event.setAttempts(4); // will become 5 on this attempt

        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        // bad JSON forces exception
        relay.processOne(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(event.getLastError()).isNotNull();

        then(outboxMetrics).should(times(1)).incrementFailed();
        then(outboxMetrics).should(times(1)).incrementDeadLetter();
        then(rabbitTemplate).should(never()).convertAndSend(any(), any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("processOne() keeps event PENDING with lastError recorded after non-final failure")
    void processOne_publishFails_retainsAsPendingWithError() throws Exception {
        TicketEmailNotificationMessage msg = TicketEmailNotificationMessage.builder().ticketId(1L).ticketNumber("TKT-1")
                .build();
        String payload = objectMapper.writeValueAsString(msg);
        OutboxEvent event = pendingEvent("TICKET_EMAIL", payload);
        event.setAttempts(1); // only 2 so far - below threshold of 5

        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));
        willThrow(new RuntimeException("broker unavailable")).given(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE), eq(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY), any(Object.class));

        relay.processOne(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(event.getLastError()).contains("broker unavailable");

        then(outboxMetrics).should(times(1)).incrementFailed();
        then(outboxMetrics).should(never()).incrementDeadLetter();
    }

    @Test
    @DisplayName("processOne() marks event FAILED for unknown event type")
    void processOne_unknownEventType_marksFailed() {
        OutboxEvent event = pendingEvent("UNKNOWN_TYPE", "{}");
        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        relay.processOne(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(event.getLastError()).contains("Unknown event type");

        then(outboxMetrics).should(times(1)).incrementDeadLetter();
    }

    private OutboxEvent pendingEvent(String eventType, String payload) {
        return OutboxEvent.builder().id(1L).eventType(eventType).aggregateType("TICKET").aggregateId(100L)
                .payload(payload).status(OutboxEvent.Status.PENDING).attempts(0).createdAt(LocalDateTime.now()).build();
    }
}
