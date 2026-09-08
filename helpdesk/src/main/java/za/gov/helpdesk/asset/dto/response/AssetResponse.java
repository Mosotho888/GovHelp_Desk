package za.gov.helpdesk.asset.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;
import za.gov.helpdesk.users.dto.response.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class AssetResponse {
    private Long id;
    private String assetTag;
    private String name;
    private AssetType type;
    private AssetStatus status;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private UserResponse assignedUser;
    private String location;
    private String vendor;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private LocalDate warrantyExpiryDate;

    /** Computed from {@link #warrantyExpiryDate} at read time - see {@code WarrantyStatus}. */
    private WarrantyStatus warrantyStatus;

    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum WarrantyStatus {
        NO_WARRANTY_INFO,
        ACTIVE,
        EXPIRING_SOON,
        EXPIRED
    }
}
