package za.gov.helpdesk.unit.services.sla;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;
import za.gov.helpdesk.notification.messaging.SlaEmailNotificationPublisher;
import za.gov.helpdesk.sla.metrics.SlaMetrics;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.sla.schedular.SlaBreachMonitor;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlaBreachMonitor unit tests")
class SlaBreachMonitorTest {

    @Mock private TicketSlaRepository ticketSlaRepository;
    @Mock private SlaPolicyRepository slaPolicyRepository;
    @Mock private SlaEmailNotificationPublisher slaEmailPublisher;
    @Mock private SlaMetrics slaMetrics;
    @Captor private ArgumentCaptor<SlaEmailNotificationMessage> messageCaptor;

    private SlaBreachMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor =
                new SlaBreachMonitor(
                        ticketSlaRepository, slaPolicyRepository, slaEmailPublisher, slaMetrics);
        // No policies configured is the common case across most tests; individual tests override
        // this where policy-specific thresholds matter.
        lenient().when(slaPolicyRepository.findAll()).thenReturn(List.of());
        lenient()
                .when(ticketSlaRepository.findResponseWarningsDue(any(), any()))
                .thenReturn(List.of());
        lenient()
                .when(ticketSlaRepository.findResolutionWarningsDue(any(), any()))
                .thenReturn(List.of());
        lenient()
                .when(ticketSlaRepository.findUnmarkedResponseBreaches(any()))
                .thenReturn(List.of());
        lenient()
                .when(ticketSlaRepository.findUnmarkedResolutionBreaches(any()))
                .thenReturn(List.of());
    }

    private TicketSla slaWithAssignedAgent(final LocalDateTime responseDueAt) {
        final User agentUser =
                User.builder().id(2L).name("Jane Agent").email("jane@gov.za").build();
        final Agent agent = Agent.builder().id(5L).user(agentUser).build();
        final Ticket ticket =
                Ticket.builder()
                        .id(100L)
                        .subject("Printer broken")
                        .priority(Priority.HIGH)
                        .assignee(agent)
                        .build();
        return TicketSla.builder().id(1L).ticket(ticket).responseDueAt(responseDueAt).build();
    }

    // ---- warnings ----

    @Test
    @DisplayName(
            "run() sends a response warning and marks it sent when within the policy's warning"
                    + " window")
    void run_responseWithinWarningWindow_sendsWarningAndMarksSent() {
        final LocalDateTime now = LocalDateTime.now();
        final TicketSla sla = slaWithAssignedAgent(now.plusMinutes(10));
        final SlaPolicy policy =
                SlaPolicy.builder().priority(Priority.HIGH).warningThresholdMinutes(30).build();

        given(slaPolicyRepository.findAll()).willReturn(List.of(policy));
        given(ticketSlaRepository.findResponseWarningsDue(any(), any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResponseWarningSent()).isTrue();
        then(ticketSlaRepository).should(times(1)).save(sla);
        then(slaMetrics).should(times(1)).incrementResponseWarning(Priority.HIGH);
        then(slaEmailPublisher).should(times(1)).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().isWarning()).isTrue();
        assertThat(messageCaptor.getValue().getDeadlineType()).isEqualTo("First Response");
    }

    @Test
    @DisplayName("run() does not warn when the due date sits outside the policy's warning window")
    void run_responseOutsideWarningWindow_doesNotWarn() {
        final LocalDateTime now = LocalDateTime.now();
        // Due in 2 hours, but the warning window is only 30 minutes - not yet due for a warning.
        final TicketSla sla = slaWithAssignedAgent(now.plusHours(2));
        final SlaPolicy policy =
                SlaPolicy.builder().priority(Priority.HIGH).warningThresholdMinutes(30).build();

        given(slaPolicyRepository.findAll()).willReturn(List.of(policy));
        given(ticketSlaRepository.findResponseWarningsDue(any(), any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResponseWarningSent()).isFalse();
        then(slaEmailPublisher).should(never()).publish(any());
        then(slaMetrics).should(never()).incrementResponseWarning(any());
    }

    @Test
    @DisplayName(
            "run() falls back to the default 30-minute threshold when no policy exists for the"
                    + " priority")
    void run_noPolicyForPriority_usesDefaultThreshold() {
        final LocalDateTime now = LocalDateTime.now();
        final TicketSla sla = slaWithAssignedAgent(now.plusMinutes(10));
        // No policies at all configured - falls back to the REMINDER default of 30 minutes.
        given(ticketSlaRepository.findResponseWarningsDue(any(), any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResponseWarningSent()).isTrue();
    }

    @Test
    @DisplayName(
            "run() skips sending a warning email for an unassigned ticket but still processes it")
    void run_warningOnUnassignedTicket_skipsEmailButMarksSent() {
        final LocalDateTime now = LocalDateTime.now();
        final Ticket unassignedTicket =
                Ticket.builder().id(200L).subject("Wifi down").priority(Priority.LOW).build();
        final TicketSla sla =
                TicketSla.builder()
                        .id(2L)
                        .ticket(unassignedTicket)
                        .responseDueAt(now.plusMinutes(5))
                        .build();
        given(ticketSlaRepository.findResponseWarningsDue(any(), any())).willReturn(List.of(sla));

        monitor.run();

        then(slaEmailPublisher).should(never()).publish(any());
        assertThat(sla.isResponseWarningSent()).isTrue();
    }

    @Test
    @DisplayName(
            "run() sends a resolution warning and marks it sent when within the warning window")
    void run_resolutionWithinWarningWindow_sendsWarningAndMarksSent() {
        final LocalDateTime now = LocalDateTime.now();
        final User agentUser =
                User.builder().id(2L).name("Jane Agent").email("jane@gov.za").build();
        final Agent agent = Agent.builder().id(5L).user(agentUser).build();
        final Ticket ticket =
                Ticket.builder()
                        .id(100L)
                        .subject("Printer broken")
                        .priority(Priority.HIGH)
                        .assignee(agent)
                        .build();
        final TicketSla sla =
                TicketSla.builder()
                        .id(1L)
                        .ticket(ticket)
                        .resolutionDueAt(now.plusMinutes(10))
                        .build();
        final SlaPolicy policy =
                SlaPolicy.builder().priority(Priority.HIGH).warningThresholdMinutes(30).build();

        given(slaPolicyRepository.findAll()).willReturn(List.of(policy));
        given(ticketSlaRepository.findResolutionWarningsDue(any(), any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResolutionWarningSent()).isTrue();
        then(slaMetrics).should(times(1)).incrementResolutionWarning(Priority.HIGH);
        then(slaEmailPublisher).should(times(1)).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getDeadlineType()).isEqualTo("Resolution");
    }

    // ---- breaches ----

    @Test
    @DisplayName("run() marks and reports every unmarked response breach")
    void run_unmarkedResponseBreach_marksAndSendsBreachNotice() {
        final TicketSla sla = slaWithAssignedAgent(LocalDateTime.now().minusHours(1));
        given(ticketSlaRepository.findUnmarkedResponseBreaches(any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResponseBreached()).isTrue();
        then(ticketSlaRepository).should(times(1)).save(sla);
        then(slaMetrics).should(times(1)).incrementResponseBreached(Priority.HIGH);
        then(slaEmailPublisher).should(times(1)).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().isWarning()).isFalse();
        assertThat(messageCaptor.getValue().getDeadlineType()).isEqualTo("First Response");
    }

    @Test
    @DisplayName("run() marks and reports every unmarked resolution breach")
    void run_unmarkedResolutionBreach_marksAndSendsBreachNotice() {
        final TicketSla sla = slaWithAssignedAgent(LocalDateTime.now());
        given(ticketSlaRepository.findUnmarkedResolutionBreaches(any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResolutionBreached()).isTrue();
        then(slaMetrics).should(times(1)).incrementResolutionBreached(Priority.HIGH);
        then(slaEmailPublisher).should(times(1)).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getDeadlineType()).isEqualTo("Resolution");
    }

    @Test
    @DisplayName(
            "run() skips sending a breach email for an unassigned ticket but still marks it"
                    + " breached")
    void run_breachOnUnassignedTicket_skipsEmailButMarksBreached() {
        final Ticket unassignedTicket =
                Ticket.builder().id(200L).subject("Wifi down").priority(Priority.LOW).build();
        final TicketSla sla =
                TicketSla.builder()
                        .id(2L)
                        .ticket(unassignedTicket)
                        .responseDueAt(LocalDateTime.now())
                        .build();
        given(ticketSlaRepository.findUnmarkedResponseBreaches(any())).willReturn(List.of(sla));

        monitor.run();

        assertThat(sla.isResponseBreached()).isTrue();
        then(slaEmailPublisher).should(never()).publish(any());
    }

    @Test
    @DisplayName("run() does nothing when there are no due warnings or unmarked breaches")
    void run_nothingDue_doesNothing() {
        monitor.run();

        then(slaEmailPublisher).should(never()).publish(any());
        then(ticketSlaRepository).should(never()).save(any());
    }
}
