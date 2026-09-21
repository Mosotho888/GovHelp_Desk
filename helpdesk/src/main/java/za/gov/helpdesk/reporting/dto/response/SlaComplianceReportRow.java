package za.gov.helpdesk.reporting.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** One day of SLA outcomes: how many tickets met their targets versus how many breached. */
@Setter
@Getter
@Builder
public class SlaComplianceReportRow {
    private LocalDate reportDate;
    private long totalCount;
    private long metCount;
    private long breachedCount;

    /**
     * Percentage of tickets that met both response and resolution targets, or null when totalCount
     * is zero.
     */
    private Double complianceRate;
}
