package za.gov.helpdesk.unit.outbox;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.config.messaging.RabbitMQConstants;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.outbox.metrics.OutboxMetrics;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxProcessor;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay unit tests")
public class OutboxProcessorTest {

    // Real ObjectMapper so JSON round-trip works
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OutboxMetrics outboxMetrics;
    @InjectMocks private OutboxProcessor processor;

    @BeforeEach
    void injectMapper() throws Exception {
        processor =
                new OutboxProcessor(outboxRepository, rabbitTemplate, objectMapper, outboxMetrics);
    }

    @Test
    @DisplayName(
            "processOne() deserialises payload, publishes to correct routing key, marks PROCESSED")
    void processOne_validTicketEmail_publishesAndMarksProcessed() throws Exception {
        final TicketEmailNotificationMessage msg =
                TicketEmailNotificationMessage.builder()
                        .ticketId(100L)
                        .ticketNumber("TKT-100")
                        .customerEmail("john@citizen.za")
                        .build();
        final String payload = objectMapper.writeValueAsString(msg);

        final OutboxEvent event = pendingEvent("TICKET_EMAIL", payload);
        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        processor.processOneSecurely(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();

        then(outboxRepository).should(times(1)).flush();
        then(outboxMetrics).should(times(1)).incrementPublished();
        then(rabbitTemplate)
                .should(times(1))
                .convertAndSend(
                        eq(RabbitMQConstants.EXCHANGE),
                        eq(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY),
                        any(TicketEmailNotificationMessage.class));
    }

    @Test
    @DisplayName("processOne() skips events that are no longer PENDING (race-condition guard)")
    void processOne_noLongerPending_skipsPublish() {
        final OutboxEvent processing = pendingEvent("TICKET_EMAIL", "{}");
        processing.setStatus(OutboxEvent.Status.PROCESSING);
        given(outboxRepository.findById(processing.getId())).willReturn(Optional.of(processing));

        processor.processOneSecurely(processing.getId());

        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("processOne() marks event FAILED after max retries (5 attempts)")
    void processOne_maxAttemptsReached_marksFailed() throws Exception {
        final OutboxEvent event = pendingEvent("TICKET_EMAIL", "{bad json}");
        event.setAttempts(4); // will become 5 on this attempt

        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        // bad JSON forces exception
        processor.processOneSecurely(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(event.getLastError()).isNotNull();

        then(outboxMetrics).should(times(1)).incrementFailed();
        then(outboxMetrics).should(times(1)).incrementDeadLetter();
        then(rabbitTemplate)
                .should(never())
                .convertAndSend(any(), any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("processOne() keeps event PENDING with lastError recorded after non-final failure")
    void processOne_publishFails_retainsAsPendingWithError() throws Exception {
        final TicketEmailNotificationMessage msg =
                TicketEmailNotificationMessage.builder().ticketId(1L).ticketNumber("TKT-1").build();
        final String payload = objectMapper.writeValueAsString(msg);
        final OutboxEvent event = pendingEvent("TICKET_EMAIL", payload);
        event.setAttempts(1); // only 2 so far - below threshold of 5

        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));
        willThrow(new RuntimeException("broker unavailable"))
                .given(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConstants.EXCHANGE),
                        eq(RabbitMQConstants.TICKET_EMAIL_ROUTING_KEY),
                        any(Object.class));

        processor.processOneSecurely(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(event.getLastError()).contains("broker unavailable");

        then(outboxMetrics).should(times(1)).incrementFailed();
        then(outboxMetrics).should(never()).incrementDeadLetter();
    }

    @Test
    @DisplayName("processOne() marks event FAILED for unknown event type")
    void processOne_unknownEventType_marksFailed() {
        final OutboxEvent event = pendingEvent("UNKNOWN_TYPE", "{}");
        given(outboxRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(outboxRepository.save(any(OutboxEvent.class))).willAnswer(i -> i.getArgument(0));

        processor.processOneSecurely(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(event.getLastError())
                .contains("Unknown domain outbox event destination type signature mapping");

        then(outboxMetrics).should(times(1)).incrementDeadLetter();
    }

    private OutboxEvent pendingEvent(final String eventType, final String payload) {
        return OutboxEvent.builder()
                .id(1L)
                .eventType(eventType)
                .aggregateType("TICKET")
                .aggregateId(100L)
                .payload(payload)
                .status(OutboxEvent.Status.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
