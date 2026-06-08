package za.gov.helpdesk.unit.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.messaging.TicketEmailNotificationPublisher;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.relay.OutboxWriter;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEmailNotificationPublisher unit tests")
class TicketEmailNotificationPublisherTest {

    @Mock
    private OutboxWriter outboxWriter;

    private TicketEmailNotificationPublisher publisher;
    private User requester;
    private User agentUser;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        publisher = new TicketEmailNotificationPublisher(outboxWriter);

        requester = User.builder().id(1L).name("John Public").email("john@citizen.za")
                .role(User.Role.USER).build();
        agentUser = User.builder().id(2L).name("Jane Agent").email("jane@gov.za")
                .role(User.Role.AGENT).build();
        Agent agent = Agent.builder().id(10L).user(agentUser).build();

        ticket = Ticket.builder()
                .id(100L).subject("Login broken").description("Cannot access dashboard")
                .status(Ticket.Status.IN_PROGRESS).priority(Ticket.Priority.HIGH)
                .requester(requester).assignee(agent)
                .build();
    }

    @Test
    @DisplayName("publish() writes a complete, correctly mapped message to the outbox")
    void publish_buildsExpectedMessage() {
        ArgumentCaptor<TicketEmailNotificationMessage> captor =
                ArgumentCaptor.forClass(TicketEmailNotificationMessage.class);

        publisher.publish(ticket, requester, agentUser, AuditLog.AuditAction.STATUS_CHANGED, "Working on it");

        then(outboxWriter).should(times(1)).write(
                eq(OutboxEvent.EventType.TICKET_EMAIL.name()),
                eq(AuditLog.EntityType.TICKET.name()),
                eq(100L),
                captor.capture()
        );

        TicketEmailNotificationMessage message = captor.getValue();
        assertThat(message.getTrigger()).isEqualTo(AuditLog.AuditAction.STATUS_CHANGED);
        assertThat(message.getTicketId()).isEqualTo(100L);
        assertThat(message.getTicketNumber()).isEqualTo("TKT-100");
        assertThat(message.getTicketSubject()).isEqualTo("Login broken");
        assertThat(message.getTicketStatus()).isEqualTo("IN_PROGRESS");
        assertThat(message.getTicketPriority()).isEqualTo("HIGH");
        assertThat(message.getComment()).isEqualTo("Working on it");
        assertThat(message.getCustomerEmail()).isEqualTo("john@citizen.za");
        assertThat(message.getCustomerName()).isEqualTo("John Public");
        assertThat(message.getAgentEmail()).isEqualTo("jane@gov.za");
        assertThat(message.getAgentName()).isEqualTo("Jane Agent");
    }

    @Test
    @DisplayName("publish() populates null agent fields when no agent is assigned")
    void publish_noAgent_nullAgentFields() {
        ArgumentCaptor<TicketEmailNotificationMessage> captor =
                ArgumentCaptor.forClass(TicketEmailNotificationMessage.class);

        publisher.publish(ticket, requester, null, AuditLog.AuditAction.TICKET_CREATED, null);

        then(outboxWriter).should(times(1)).write(
                eq(OutboxEvent.EventType.TICKET_EMAIL.name()),
                eq(AuditLog.EntityType.TICKET.name()),
                eq(100L),
                captor.capture()
        );

        TicketEmailNotificationMessage msg = captor.getValue();
        assertThat(msg.getAgentEmail()).isNull();
        assertThat(msg.getAgentName()).isNull();
    }

    @Test
    @DisplayName("publish() swallows outbox failures and does not propagate the exception")
    void publish_outboxFailure_doesNotThrow() {
        doThrow(new IllegalStateException("Failed to write outbox event"))
                .when(outboxWriter).write(
                        eq(OutboxEvent.EventType.TICKET_EMAIL.name()),
                        eq(AuditLog.EntityType.TICKET.name()),
                        eq(100L),
                        any(TicketEmailNotificationMessage.class)
                );

        // Must not throw — publisher swallows broker / outbox errors
        publisher.publish(ticket, requester, agentUser, AuditLog.AuditAction.STATUS_CHANGED, null);

        then(outboxWriter).should(times(1)).write(
                eq(OutboxEvent.EventType.TICKET_EMAIL.name()),
                eq(AuditLog.EntityType.TICKET.name()),
                eq(100L),
                any(TicketEmailNotificationMessage.class)
        );
    }

    @Test
    @DisplayName("publish() uses trigger from AuditAction verbatim in the message")
    void publish_triggerMappedCorrectly_forEachRelevantAction() {
        for (AuditLog.AuditAction action : new AuditLog.AuditAction[]{
                AuditLog.AuditAction.TICKET_CREATED,
                AuditLog.AuditAction.ASSIGNED_TO_AGENT,
                AuditLog.AuditAction.STATUS_CHANGED,
                AuditLog.AuditAction.TICKET_CLOSED
        }) {
            ArgumentCaptor<TicketEmailNotificationMessage> captor =
                    ArgumentCaptor.forClass(TicketEmailNotificationMessage.class);

            publisher.publish(ticket, requester, agentUser, action, null);

            then(outboxWriter).should(times(1)).write(
                    eq(OutboxEvent.EventType.TICKET_EMAIL.name()),
                    eq(AuditLog.EntityType.TICKET.name()),
                    eq(100L),
                    captor.capture()
            );
            assertThat(captor.getValue().getTrigger()).isEqualTo(action);

            org.mockito.Mockito.clearInvocations(outboxWriter);
        }
    }
}
