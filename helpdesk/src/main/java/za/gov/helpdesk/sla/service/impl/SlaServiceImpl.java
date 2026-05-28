package za.gov.helpdesk.sla.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.notification.service.sla.SlaEmailService;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.sla.service.BusinessHoursCalculator;
import za.gov.helpdesk.sla.service.SlaService;
import za.gov.helpdesk.ticket.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaServiceImpl implements SlaService {

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final BusinessHoursCalculator calculator;
    private final SlaEmailService emailService;

    @Override
    @Transactional
    public TicketSla initializeSla(Ticket ticket) {
        SlaPolicy policy = slaPolicyRepository
                .findByPriority(ticket.getPriority())
                .orElseThrow(() -> new IllegalStateException(
                        "No SLA policy for priority: " + ticket.getPriority()));

        LocalDateTime now           = LocalDateTime.now();
        LocalDateTime responseDue   = calculator.addBusinessMinutes(now, policy.getResponseMinutes());
        LocalDateTime resolutionDue = calculator.addBusinessMinutes(now, policy.getResolutionMinutes());

        TicketSla sla = TicketSla.builder()
                .ticket(ticket)
                .responseDueAt(responseDue)
                .resolutionDueAt(resolutionDue)
                .build();

        log.info("SLA initialized: ticket={} responseDue={} resolutionDue={}",
                ticket.getId(), responseDue, resolutionDue);

        return ticketSlaRepository.save(sla);
    }

    @Override
    @Transactional
    public void recordFirstResponse(Long ticketId) {
        ticketSlaRepository.findByTicketId(ticketId).ifPresent(sla -> {
            if (sla.getFirstResponseAt() == null) {
                sla.setFirstResponseAt(LocalDateTime.now());
                sla.setResponseBreached(LocalDateTime.now().isAfter(sla.getResponseDueAt()));
                ticketSlaRepository.save(sla);
                log.info("First response recorded: ticket={} breached={}",
                        ticketId, sla.isResponseBreached());
            }
        });
    }

    @Override
    public void recordResolution(Long ticketId) {
        ticketSlaRepository.findByTicketId(ticketId).ifPresent(sla -> {
            if (sla.getResolvedAt() == null) {
                sla.setResolvedAt(LocalDateTime.now());
                sla.setResolutionBreached(
                        LocalDateTime.now().isAfter(sla.getResolutionDueAt()));
                ticketSlaRepository.save(sla);
                log.info("Resolution recorded: ticket={} breached={}",
                        ticketId, sla.isResolutionBreached());
            }
        });
    }

    @Scheduled(fixedRateString = "PT5M")
    @Transactional
    public void checkSlaWarnings() {
        LocalDateTime now = LocalDateTime.now();
        Map<Ticket.Priority, SlaPolicy> policiesByPriority = slaPolicyRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SlaPolicy::getPriority, Function.identity()));
        int maxWarningThresholdMinutes = policiesByPriority.values()
                .stream()
                .mapToInt(SlaPolicy::getWarningThresholdMinutes)
                .max()
                .orElse(30);

        // Response warnings
        List<TicketSla> responseWarnings =
                ticketSlaRepository.findResponseWarningsDue(now, now.plusMinutes(maxWarningThresholdMinutes));

        for (TicketSla sla : responseWarnings) {
            if (!isWarningDue(sla, policiesByPriority, sla.getResponseDueAt(), now)) continue;
            sendWarning(sla, "First Response");
            sla.setResponseWarningSent(true);
            ticketSlaRepository.save(sla);
        }

        // Resolution warnings
        List<TicketSla> resolutionWarnings =
                ticketSlaRepository.findResolutionWarningsDue(now, now.plusMinutes(maxWarningThresholdMinutes));

        for (TicketSla sla : resolutionWarnings) {
            if (!isWarningDue(sla, policiesByPriority, sla.getResolutionDueAt(), now)) continue;
            sendWarning(sla, "Resolution");
            sla.setResolutionWarningSent(true);
            ticketSlaRepository.save(sla);
        }

        // Mark response breaches
        ticketSlaRepository.findUnmarkedResponseBreaches(now).forEach(sla -> {
            sla.setResponseBreached(true);
            ticketSlaRepository.save(sla);
            sendBreach(sla, "First Response");
            log.warn("Response SLA breached: ticket={}", sla.getTicket().getId());
        });

        // Mark resolution breaches
        ticketSlaRepository.findUnmarkedResolutionBreaches(now).forEach(sla -> {
            sla.setResolutionBreached(true);
            ticketSlaRepository.save(sla);
            sendBreach(sla, "Resolution");
            log.warn("Resolution SLA breached: ticket={}", sla.getTicket().getId());
        });
    }

    private boolean isWarningDue(TicketSla sla,
                                 Map<Ticket.Priority, SlaPolicy> policiesByPriority,
                                 LocalDateTime dueAt,
                                 LocalDateTime now) {
        SlaPolicy policy = policiesByPriority.get(sla.getTicket().getPriority());
        int thresholdMinutes = policy != null ? policy.getWarningThresholdMinutes() : 30;
        return !dueAt.isAfter(now.plusMinutes(thresholdMinutes));
    }

    private void sendWarning(TicketSla sla, String deadlineType) {
        Ticket ticket = sla.getTicket();
        if (ticket.getAssignee() == null) return;

        String agentEmail = ticket.getAssignee().getUser().getEmail();
        String agentName  = ticket.getAssignee().getUser().getName();

        LocalDateTime dueAt = "First Response".equals(deadlineType)
                ? sla.getResponseDueAt()
                : sla.getResolutionDueAt();

        emailService.sendSlaWarning(
                agentEmail, agentName,
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
