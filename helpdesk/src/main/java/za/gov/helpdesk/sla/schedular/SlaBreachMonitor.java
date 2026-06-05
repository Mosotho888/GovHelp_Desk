package za.gov.helpdesk.sla.schedular;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.ticket.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaBreachMonitor {

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaEmailService emailService;

    @Scheduled(fixedRateString = "PT5M")
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        Map<Ticket.Priority, SlaPolicy> policies = slaPolicyRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SlaPolicy::getPriority, Function.identity()));

        int maxThreshold = policies.values().stream()
                .mapToInt(SlaPolicy::getWarningThresholdMinutes)
                .max()
                .orElse(30);

        processWarnings(now, policies, maxThreshold);
        processBreaches(now);
    }

    private void processWarnings(LocalDateTime now,
                                 Map<Ticket.Priority, SlaPolicy> policies,
                                 int maxThreshold) {

        List<TicketSla> responseWarnings = ticketSlaRepository
                .findResponseWarningsDue(now, now.plusMinutes(maxThreshold));

        for (TicketSla sla : responseWarnings) {
            if (!isWithinWarningWindow(sla, policies, sla.getResponseDueAt(), now)) {
                continue;
            }
            sendWarning(sla, "First Response");
            sla.setResponseWarningSent(true);
            ticketSlaRepository.save(sla);
        }

        List<TicketSla> resolutionWarnings = ticketSlaRepository
                .findResolutionWarningsDue(now, now.plusMinutes(maxThreshold));

        for (TicketSla sla : resolutionWarnings) {
            if (isWithinWarningWindow(sla, policies, sla.getResolutionDueAt(), now)) {
                continue;
            }
            sendWarning(sla, "Resolution");
            sla.setResolutionWarningSent(true);
            ticketSlaRepository.save(sla);
        }
    }

    private void processBreaches(LocalDateTime now) {

        ticketSlaRepository.findUnmarkedResponseBreaches(now).forEach(sla -> {
            sla.setResponseBreached(true);
            ticketSlaRepository.save(sla);
            sendBreach(sla, "First Response");
            log.warn("Response SLA breached: ticket={}", sla.getTicket().getId());
        });

        ticketSlaRepository.findUnmarkedResolutionBreaches(now).forEach(sla -> {
            sla.setResolutionBreached(true);
            ticketSlaRepository.save(sla);
            sendBreach(sla, "Resolution");
            log.warn("Resolution SLA breached: ticket={}", sla.getTicket().getId());
        });
    }

    private boolean isWithinWarningWindow(TicketSla sla,
                                          Map<Ticket.Priority, SlaPolicy> policies,
                                          LocalDateTime dueAt,
                                          LocalDateTime now) {
        SlaPolicy policy = policies.get(sla.getTicket().getPriority());
        int thresholdMinutes = policy != null ? policy.getWarningThresholdMinutes() : 30;
        return dueAt.isAfter(now.plusMinutes(thresholdMinutes));
    }

    private void sendWarning(TicketSla sla, String deadlineType) {
        Ticket ticket = sla.getTicket();
        if (ticket.getAssignee() == null) return;

        LocalDateTime dueAt = "First Response".equals(deadlineType)
                ? sla.getResponseDueAt()
                : sla.getResolutionDueAt();

        emailService.sendSlaWarning(
                ticket.getAssignee().getUser().getEmail(),
                ticket.getAssignee().getUser().getName(),
                "TKT-" + ticket.getId(),
                ticket.getSubject(),
                deadlineType,
                dueAt
        );
    }

    private void sendBreach(TicketSla sla, String deadlineType) {
        Ticket ticket = sla.getTicket();
        if (ticket.getAssignee() == null) return;

        emailService.sendSlaBreach(
                ticket.getAssignee().getUser().getEmail(),
                ticket.getAssignee().getUser().getName(),
                "TKT-" + ticket.getId(),
                ticket.getSubject(),
                deadlineType
        );
    }
}
