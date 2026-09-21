package za.gov.helpdesk.reporting.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import za.gov.helpdesk.reporting.service.ReportingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Keeps the reporting materialised views current on a fixed cycle, so dashboards never fall too far
 * behind live data without every request having to compute the aggregation itself. Refreshing runs
 * outside any request or transaction, matching how ReportingRepository.refreshAll() issues each
 * REFRESH MATERIALIZED VIEW CONCURRENTLY statement on its own.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingRefreshScheduler {

    private final ReportingService reportingService;

    /**
     * Triggers a refresh of every reporting materialized view every fifteen minutes, measured from
     * the start of the previous run.
     */
    @Scheduled(fixedRateString = "PT15M")
    public void run() {
        log.info("Refreshing reporting materialized views");
        reportingService.refreshNow();
        log.info("Reporting materialized views refreshed");
    }
}
