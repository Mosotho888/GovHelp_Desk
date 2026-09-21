package za.gov.helpdesk.reporting.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Ticket volume and average resolution time for a single ticket category. */
@Setter
@Getter
@Builder
public class CategoryBreakdownReportRow {
    private Long categoryId;
    private String categoryName;
    private long ticketCount;
    private Double avgResolutionHours;
}
