package za.gov.helpdesk.outbox.relay;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.outbox.metrics.OutboxMetrics;
import za.gov.helpdesk.outbox.model.OutboxEvent;
import za.gov.helpdesk.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled coordination component acting as the runtime pulse for the Transactional Outbox Pattern
 * engine. Periodically polls the persistent outbox store to sweep pending messages into highly
 * isolated execution pipelines, registers active monitoring gauge metrics, and implements data
 * housekeeping policies via automated cron purges.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH_SIZE = 50;
    private static final int DAYS = 7;

    private final OutboxEventRepository outboxRepository;
    private final OutboxMetrics outboxMetrics;
    private final OutboxProcessor outboxProcessor;

    /**
     * Executes the primary event relay pipeline loop on a fixed interval delay pattern. Extracts an
     * identity batch of outstanding pending logs and delegates individual item processing out to an
     * adjacent proxy-wrapped processor to preserve explicit transaction fragmentation. Updates
     * active system metric gauges to accurately observe system backpressure.
     */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT5S}")
    public void relay() {
        try {
            // Invoking via outboxProcessor bean triggers Spring Proxy Transaction interceptors
            // cleanly!
            final List<Long> pendingIds = outboxProcessor.fetchNextPendingIdsBatch(BATCH_SIZE);

            final long pending = outboxRepository.countByStatus(OutboxEvent.Status.PENDING);
            outboxMetrics.setPendingGauge(pending);

            if (pendingIds.isEmpty()) {
                return;
            }

            log.debug("Outbox relay: processing {} events", pendingIds.size());

            for (final Long eventId : pendingIds) {
                try {
                    outboxProcessor.processOneSecurely(eventId);
                } catch (final Exception ex) {
                    log.error(
                            "Fatal transaction crash handled isolated to outbox record ID: {}",
                            eventId,
                            ex);
                }
            }
        } catch (final Exception ex) {
            log.error("Outbox relay processing batch collection polling routine failed", ex);
        }
    }

    /**
     * Runs an early-morning database maintenance housekeeping cron task to clean out historic
     * transaction logs. Evaluates records that have already transitioned into a finalized success
     * state and executes an atomic purge of any entry exceeding the established timeline retention
     * threshold interval.
     */
    @Scheduled(cron = "${app.outbox.purge-cron:0 0 3 * * *}")
    @Transactional
    public void purgeProcessed() {
        final int deleted =
                outboxRepository.deleteProcessedBefore(LocalDateTime.now().minusDays(DAYS));
        if (deleted > 0) {
            log.info("Outbox purge: deleted {} processed events", deleted);
        }
    }
}
