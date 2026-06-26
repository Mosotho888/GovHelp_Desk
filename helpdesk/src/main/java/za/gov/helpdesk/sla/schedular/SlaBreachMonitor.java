package za.gov.helpdesk.sla.schedular;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.notification.messaging.SlaEmailNotificationPublisher;
import za.gov.helpdesk.sla.metrics.SlaMetrics;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.ticket.model.Ticket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled monitoring monitor component responsible for tracking and auditing active Ticket SLA
 * deadlines. Scans the database at configured intervals to evaluate time differentials, flags
 * approaching warning windows or outright deadline breaches for response and resolution targets,
 * and triggers downstream notification events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlaBreachMonitor {

    private static final int REMINDER = 30;

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaEmailNotificationPublisher slaEmailPublisher;
    private final SlaMetrics slaMetrics;

    /**
     * Executes the main SLA assessment loop on a periodic cron or rate interval. Caches priority
     * configuration rules, identifies the global maximum checking interval boundary to constrain
     * page scan sweeps, and triggers internal warning and breach assessment routines.
     */
    @Scheduled(fixedRateString = "PT5M")
    @Transactional
    public void run() {
        final LocalDateTime now = LocalDateTime.now();

        final Map<Ticket.Priority, SlaPolicy> policies =
                slaPolicyRepository.findAll().stream()
                        .collect(Collectors.toMap(SlaPolicy::getPriority, Function.identity()));

        final int maxThreshold =
                policies.values().stream()
                        .mapToInt(SlaPolicy::getWarningThresholdMinutes)
                        .max()
                        .orElse(REMINDER);

        processWarnings(now, policies, maxThreshold);
        processBreaches(now);
    }

    /**
     * Inspects active ticket tracking objects for approaching response or resolution deadline
     * milestones. Evaluates row items found within the outer time threshold constraint, verifies if
     * they reside inside their specific policy warning window, dispatches alerts, and persists
     * state markers.
     *
     * @param now the current baseline system timestamp tracking the execution point
     * @param policies a mapped collection tracking active policy threshold metrics grouped by
     *     priority tiers
     * @param maxThreshold the maximum numerical minute window configuration found across all active
     *     priority rules
     */
    private void processWarnings(
            final LocalDateTime now,
            final Map<Ticket.Priority, SlaPolicy> policies,
            final int maxThreshold) {

        final List<TicketSla> responseWarnings =
                ticketSlaRepository.findResponseWarningsDue(now, now.plusMinutes(maxThreshold));

        for (final TicketSla sla : responseWarnings) {
            if (!isWithinWarningWindow(sla, policies, sla.getResponseDueAt(), now)) {
                continue;
            }
            sendWarning(sla, "First Response");
            sla.setResponseWarningSent(true);
            ticketSlaRepository.save(sla);
            slaMetrics.incrementResponseWarning();
        }

        final List<TicketSla> resolutionWarnings =
                ticketSlaRepository.findResolutionWarningsDue(now, now.plusMinutes(maxThreshold));

        for (final TicketSla sla : resolutionWarnings) {
            if (isWithinWarningWindow(sla, policies, sla.getResolutionDueAt(), now)) {
                continue;
            }
            sendWarning(sla, "Resolution");
            sla.setResolutionWarningSent(true);
            ticketSlaRepository.save(sla);
            slaMetrics.incrementResolutionWarning();
        }
    }

    /**
     * Identifies active ticket tracking lines that have entirely outlasted their assigned response
     * or resolution target windows without completion, applying permanent database breach state
     * flags.
     *
     * @param now the current baseline system execution timestamp context
     */
    private void processBreaches(final LocalDateTime now) {

        ticketSlaRepository
                .findUnmarkedResponseBreaches(now)
                .forEach(
                        sla -> {
                            sla.setResponseBreached(true);
                            ticketSlaRepository.save(sla);
                            sendBreach(sla, "First Response");
                            slaMetrics.incrementResponseBreached();
                            log.warn("Response SLA breached: ticket={}", sla.getTicket().getId());
                        });

        ticketSlaRepository
                .findUnmarkedResolutionBreaches(now)
                .forEach(
                        sla -> {
                            sla.setResolutionBreached(true);
                            ticketSlaRepository.save(sla);
                            sendBreach(sla, "Resolution");
                            slaMetrics.incrementResolutionBreached();
                            log.warn("Resolution SLA breached: ticket={}", sla.getTicket().getId());
                        });
    }

    /**
     * Assesses whether a specific deadline timestamp sits inside the policy warning threshold
     * relative to the current tracking runtime clock.
     *
     * @param sla the domain tracker state holding parent priority information
     * @param policies the system map container caching policy details
     * @param dueAt the absolute target expiration timestamp boundary to check against
     * @param now the current baseline time context
     * @return true if the execution window matches the warning threshold, false otherwise
     */
    private boolean isWithinWarningWindow(
            final TicketSla sla,
            final Map<Ticket.Priority, SlaPolicy> policies,
            final LocalDateTime dueAt,
            final LocalDateTime now) {
        final SlaPolicy policy = policies.get(sla.getTicket().getPriority());
        final int thresholdMinutes =
                policy != null ? policy.getWarningThresholdMinutes() : REMINDER;
        return dueAt.isAfter(now.plusMinutes(thresholdMinutes));
    }

    /**
     * Constructs and dispatches an asynchronous warning alert via the outbox publisher mechanism.
     * Gracefully aborts operations if the target parent ticket does not currently have an assigned
     * support agent.
     *
     * @param sla the domain target tracking context containing ticket references
     * @param deadlineType a text classification string mapping either to "First Response" or
     *     "Resolution"
     */
    private void sendWarning(final TicketSla sla, final String deadlineType) {
        final Ticket ticket = sla.getTicket();
        if (ticket.getAssignee() == null) {
            return;
        }

        final LocalDateTime dueAt =
                "First Response".equals(deadlineType)
                        ? sla.getResponseDueAt()
                        : sla.getResolutionDueAt();

        slaEmailPublisher.publishWarning(
                ticket.getAssignee().getUser().getEmail(),
                ticket.getAssignee().getUser().getName(),
                "TKT-" + ticket.getId(),
                ticket.getId(),
                ticket.getSubject(),
                deadlineType,
                dueAt);
    }

    /**
     * Constructs and dispatches an asynchronous final SLA breach escalation notice via the outbox
     * layer. Gracefully exits execution trajectories if no support agent is linked to the ticket
     * entity.
     *
     * @param sla the domain target tracking context containing ticket references
     * @param deadlineType a text classification string mapping either to "First Response" or
     *     "Resolution"
     */
    private void sendBreach(final TicketSla sla, final String deadlineType) {
        final Ticket ticket = sla.getTicket();
        if (ticket.getAssignee() == null) {
            return;
        }

        slaEmailPublisher.publishBreach(
                ticket.getAssignee().getUser().getEmail(),
                ticket.getAssignee().getUser().getName(),
                "TKT-" + ticket.getId(),
                ticket.getId(),
                ticket.getSubject(),
                deadlineType);
    }
}
