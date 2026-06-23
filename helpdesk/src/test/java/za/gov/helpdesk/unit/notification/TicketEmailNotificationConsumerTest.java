package za.gov.helpdesk.unit.notification;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationConsumer;
import za.gov.helpdesk.notification.metrics.NotificationMetrics;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEmailNotificationConsumer unit tests")
class TicketEmailNotificationConsumerTest {

    @Mock private TicketEmailService ticketEmailService;
    @Mock private NotificationMetrics notificationMetrics;
    @Mock private Channel channel;

    private TicketEmailNotificationConsumer consumer;
    private Message rawMessage;

    @BeforeEach
    void setUp() {
        consumer = new TicketEmailNotificationConsumer(ticketEmailService, notificationMetrics);
        final MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(42L);
        rawMessage = new Message(new byte[0], properties);
    }

    @Test
    @DisplayName("handle() routes supported triggers and ACKs")
    void handle_routesSupportedTriggersAndAcks() throws Exception {
        for (final AuditLog.AuditAction trigger :
                Stream.of(
                                AuditLog.AuditAction.TICKET_CREATED,
                                AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                                AuditLog.AuditAction.STATUS_CHANGED,
                                AuditLog.AuditAction.COMMENT_ADDED,
                                AuditLog.AuditAction.TICKET_CLOSED)
                        .toList()) {
            final TicketEmailNotificationMessage message = message(trigger);

            consumer.handle(message, rawMessage, channel);

            then(channel).should(times(1)).basicAck(42L, false);
            org.mockito.Mockito.clearInvocations(channel);
        }

        then(ticketEmailService)
                .should(times(1))
                .sendTicketCreated(org.mockito.ArgumentMatchers.any());
        then(ticketEmailService)
                .should(times(1))
                .sendTicketAssigned(org.mockito.ArgumentMatchers.any());
        then(ticketEmailService)
                .should(times(1))
                .sendStatusChanged(org.mockito.ArgumentMatchers.any());
        then(ticketEmailService)
                .should(times(1))
                .sendCommentAdded(org.mockito.ArgumentMatchers.any());
        then(ticketEmailService)
                .should(times(1))
                .sendTicketClosed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("handle() ACKs unsupported triggers without email handler")
    void handle_unsupportedTrigger_acksWithoutRouting() throws Exception {
        final TicketEmailNotificationMessage message =
                message(AuditLog.AuditAction.PRIORITY_CHANGED);

        consumer.handle(message, rawMessage, channel);

        then(channel).should(times(1)).basicAck(42L, false);
        then(ticketEmailService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("handle() NACKs and requeues when email sending fails")
    void handle_emailFailure_nacksAndRequeues() throws Exception {
        final TicketEmailNotificationMessage message = message(AuditLog.AuditAction.TICKET_CREATED);
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(ticketEmailService)
                .sendTicketCreated(message);

        consumer.handle(message, rawMessage, channel);

        then(channel).should(times(1)).basicNack(42L, false, true);
    }

    @Test
    @DisplayName("handle() NACKs when channel ACK fails")
    void handle_channelAckFailure_nacksAndRequeues() throws Exception {
        final TicketEmailNotificationMessage message = message(AuditLog.AuditAction.TICKET_CREATED);
        final Channel failingChannel = mock(Channel.class);
        doThrow(new java.io.IOException("ack failed")).when(failingChannel).basicAck(42L, false);

        consumer.handle(message, rawMessage, failingChannel);

        then(failingChannel).should(times(1)).basicNack(42L, false, true);
    }

    private TicketEmailNotificationMessage message(final AuditLog.AuditAction trigger) {
        return TicketEmailNotificationMessage.builder()
                .trigger(trigger)
                .ticketNumber("TKT-100")
                .build();
    }
}
