package za.gov.helpdesk.unit.services.sla;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.sla.dto.TicketSlaResponse;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.sla.service.BusinessHoursCalculator;
import za.gov.helpdesk.sla.service.SlaQueryHelper;
import za.gov.helpdesk.sla.service.impl.SlaServiceImpl;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlaServiceImpl unit tests")
class SlaServiceImplTest {

    @Mock private TicketSlaRepository ticketSlaRepository;
    @Mock private SlaPolicyRepository slaPolicyRepository;
    @Mock private SlaQueryHelper slaQuery;
    @Mock private BusinessHoursCalculator calculator;

    @InjectMocks private SlaServiceImpl slaService;

    private User requester;
    private User agentUser;
    private Agent agent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        requester =
                User.builder()
                        .id(1L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(User.Role.USER)
                        .active(true)
                        .build();
        agentUser =
                User.builder()
                        .id(2L)
                        .name("Jane Agent")
                        .email("jane@gov.za")
                        .role(User.Role.AGENT)
                        .active(true)
                        .build();
        agent = Agent.builder().id(10L).user(agentUser).build();
        ticket =
                Ticket.builder()
                        .id(100L)
                        .subject("Login broken")
                        .description("Cannot access dashboard")
                        .priority(Ticket.Priority.HIGH)
                        .requester(requester)
                        .assignee(agent)
                        .build();
    }

    @Test
    @DisplayName("initializeSla() saves SLA with policy-derived deadlines")
    void initializeSla_usesPolicyDeadlines() {
        final SlaPolicy policy =
                SlaPolicy.builder()
                        .priority(Ticket.Priority.HIGH)
                        .responseMinutes(240)
                        .resolutionMinutes(480)
                        .build();
        final LocalDateTime responseDue = LocalDateTime.now().plusHours(4);
        final LocalDateTime resolutionDue = LocalDateTime.now().plusHours(8);

        given(slaQuery.getPolicyOrThrow(Ticket.Priority.HIGH)).willReturn(policy);
        given(calculator.addBusinessMinutes(any(LocalDateTime.class), eq(240L)))
                .willReturn(responseDue);
        given(calculator.addBusinessMinutes(any(LocalDateTime.class), eq(480L)))
                .willReturn(resolutionDue);
        given(ticketSlaRepository.save(any(TicketSla.class))).willAnswer(i -> i.getArgument(0));

        final TicketSla result = slaService.initializeSla(ticket);

        assertThat(result.getTicket()).isEqualTo(ticket);
        assertThat(result.getResponseDueAt()).isEqualTo(responseDue);
        assertThat(result.getResolutionDueAt()).isEqualTo(resolutionDue);
        then(ticketSlaRepository).should(times(1)).save(any(TicketSla.class));
    }

    @Test
    @DisplayName("initializeSla() throws clearly when no SLA policy exists for priority")
    void initializeSla_missingPolicy_throwsIllegalState() {
        given(slaQuery.getPolicyOrThrow(Ticket.Priority.HIGH))
                .willThrow(
                        new IllegalStateException(
                                "No SLA policy configured for system priority: HIGH"));

        assertThatThrownBy(() -> slaService.initializeSla(ticket))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No SLA policy");

        then(ticketSlaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("recordFirstResponse() records timestamp and marks breached when late")
    void recordFirstResponse_whenLate_marksBreached() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().minusMinutes(5))
                        .resolutionDueAt(LocalDateTime.now().plusHours(1))
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordFirstResponse(100L);

        assertThat(sla.getFirstResponseAt()).isNotNull();
        assertThat(sla.isResponseBreached()).isTrue();
        then(ticketSlaRepository).should(times(1)).save(sla);
    }

    @Test
    @DisplayName("recordFirstResponse() records timestamp and marks not-breached when on time")
    void recordFirstResponse_onTime_notBreached() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().plusHours(2))
                        .resolutionDueAt(LocalDateTime.now().plusHours(8))
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordFirstResponse(100L);

        assertThat(sla.getFirstResponseAt()).isNotNull();
        assertThat(sla.isResponseBreached()).isFalse();
        then(ticketSlaRepository).should(times(1)).save(sla);
    }

    @Test
    @DisplayName("recordFirstResponse() does not overwrite an existing first-response timestamp")
    void recordFirstResponse_existingTimestamp_doesNothing() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().minusMinutes(5))
                        .resolutionDueAt(LocalDateTime.now().plusHours(1))
                        .firstResponseAt(LocalDateTime.now().minusMinutes(2)) // already set
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordFirstResponse(100L);

        then(ticketSlaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("recordFirstResponse() is a no-op when no SLA record exists")
    void recordFirstResponse_noSlaRecord_doesNothing() {
        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.empty());

        slaService.recordFirstResponse(100L); // must not throw

        then(ticketSlaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("recordResolution() records timestamp and marks breached when late")
    void recordResolution_whenLate_marksBreached() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().minusHours(1))
                        .resolutionDueAt(LocalDateTime.now().minusMinutes(5))
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordResolution(100L);

        assertThat(sla.getResolvedAt()).isNotNull();
        assertThat(sla.isResolutionBreached()).isTrue();
        then(ticketSlaRepository).should(times(1)).save(sla);
    }

    @Test
    @DisplayName("recordResolution() records timestamp and marks not-breached when on time")
    void recordResolution_onTime_notBreached() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().minusHours(1))
                        .resolutionDueAt(LocalDateTime.now().plusHours(3))
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordResolution(100L);

        assertThat(sla.getResolvedAt()).isNotNull();
        assertThat(sla.isResolutionBreached()).isFalse();
        then(ticketSlaRepository).should(times(1)).save(sla);
    }

    @Test
    @DisplayName("recordResolution() does not overwrite an existing resolved timestamp")
    void recordResolution_existingTimestamp_doesNothing() {
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().minusHours(1))
                        .resolutionDueAt(LocalDateTime.now().minusMinutes(5))
                        .resolvedAt(LocalDateTime.now().minusMinutes(3)) // already set
                        .build();

        given(ticketSlaRepository.findByTicketId(100L)).willReturn(Optional.of(sla));

        slaService.recordResolution(100L);

        then(ticketSlaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("getSlaStatus() returns ON_TRACK when well within deadline")
    void getSlaStatus_onTrack_returnsOnTrack() {
        final SlaPolicy policy =
                SlaPolicy.builder()
                        .priority(Ticket.Priority.HIGH)
                        .responseMinutes(240)
                        .resolutionMinutes(480)
                        .warningThresholdMinutes(30)
                        .build();
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().plusHours(3))
                        .resolutionDueAt(LocalDateTime.now().plusHours(6))
                        .build();

        given(slaQuery.findByTicketOrThrow(100L)).willReturn(sla);
        given(slaPolicyRepository.findByPriority(Ticket.Priority.HIGH))
                .willReturn(Optional.of(policy));

        final TicketSlaResponse response = slaService.getSlaStatus(100L);

        assertThat(response.getStatus()).isEqualTo("ON_TRACK");
        assertThat(response.isResponseBreached()).isFalse();
        assertThat(response.isResolutionBreached()).isFalse();
    }

    @Test
    @DisplayName("getSlaStatus() returns AT_RISK when inside warning threshold")
    void getSlaStatus_withinWarningThreshold_returnsAtRisk() {
        final SlaPolicy policy =
                SlaPolicy.builder()
                        .priority(Ticket.Priority.HIGH)
                        .responseMinutes(240)
                        .resolutionMinutes(480)
                        .warningThresholdMinutes(60)
                        .build();
        final TicketSla sla =
                TicketSla.builder()
                        .ticket(ticket)
                        .responseDueAt(LocalDateTime.now().plusHours(3))
                        .resolutionDueAt(
                                LocalDateTime.now().plusMinutes(30)) // within 60-min warning window
                        .build();

        given(slaQuery.findByTicketOrThrow(100L)).willReturn(sla);
        given(slaPolicyRepository.findByPriority(Ticket.Priority.HIGH))
                .willReturn(Optional.of(policy));

        final TicketSlaResponse response = slaService.getSlaStatus(100L);

        assertThat(response.getStatus()).isEqualTo("AT_RISK");
    }

    @Test
    @DisplayName("getSlaStatus() throws ResourceNotFoundException when no SLA record exists")
    void getSlaStatus_noSlaRecord_throwsNotFound() {
        given(slaQuery.findByTicketOrThrow(999L))
                .willThrow(new ResourceNotFoundException("SLA metadata for ticket", 999L));

        assertThatThrownBy(() -> slaService.getSlaStatus(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
