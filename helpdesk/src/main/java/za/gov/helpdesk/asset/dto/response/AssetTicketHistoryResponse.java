package za.gov.helpdesk.asset.dto.response;

import java.time.LocalDateTime;

import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;

import lombok.Builder;
import lombok.Data;

/**
 * A single entry in an asset's device history - deliberately lighter than the full {@code
 * TicketResponse}, since a technician scanning an asset's history wants the ticket's shape at a
 * glance, not its category/assignee/etc. hydrated in full.
 */
@Data
@Builder
public class AssetTicketHistoryResponse {
    private Long ticketId;
    private String subject;
    private Status status;
    private Priority priority;
    private LocalDateTime linkedAt;
}
