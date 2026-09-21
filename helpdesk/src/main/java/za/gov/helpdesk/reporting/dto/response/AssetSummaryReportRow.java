package za.gov.helpdesk.reporting.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Inventory counts for one asset type/status combination, plus warranty expiry buckets. */
@Setter
@Getter
@Builder
public class AssetSummaryReportRow {
    private String type;
    private String status;
    private long assetCount;
    private long warrantyExpiringSoonCount;
    private long warrantyExpiredCount;
}
