package za.gov.helpdesk.reporting.dto.response;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** One row of daily ticket volume, broken down by status and priority. */
@Setter
@Getter
@Builder
public class TicketVolumeReportRow {
    private LocalDate reportDate;
    private String status;
    private String priority;
    private long ticketCount;
}
