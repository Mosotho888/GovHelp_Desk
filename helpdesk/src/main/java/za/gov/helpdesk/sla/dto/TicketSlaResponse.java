package za.gov.helpdesk.sla.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSlaResponse {

    private LocalDateTime responseDueAt;
    private LocalDateTime resolutionDueAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private boolean       responseBreached;
    private boolean       resolutionBreached;
    private String        status;          // ON_TRACK, AT_RISK, BREACHED
}
