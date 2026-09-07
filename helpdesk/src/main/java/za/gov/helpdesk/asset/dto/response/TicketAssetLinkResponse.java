package za.gov.helpdesk.asset.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketAssetLinkResponse {
    private Long ticketId;
    private Long assetId;
    private String assetTag;
    private String assetName;
    private String linkedByName;
    private LocalDateTime linkedAt;
}
