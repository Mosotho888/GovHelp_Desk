package za.gov.helpdesk.asset.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetRequest {

    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    private AssetType type;

    private AssetStatus status;

    @Size(max = 100)
    private String serialNumber;

    @Size(max = 100)
    private String manufacturer;

    @Size(max = 100)
    private String model;

    /** Reassigns the asset to a different user. Ignored if {@link #clearAssignedUser} is true. */
    private Long assignedUserId;

    /**
     * Explicitly unassigns the asset (sets assignedUser to null). A plain {@code null} on {@link
     * #assignedUserId} is indistinguishable from "field omitted" over JSON, so clearing an
     * assignment requires this separate flag rather than overloading the id field.
     */
    private boolean clearAssignedUser;

    @Size(max = 150)
    private String location;

    @Size(max = 150)
    private String vendor;

    private LocalDate purchaseDate;

    @DecimalMin(value = "0.0", message = "Purchase cost cannot be negative")
    private BigDecimal purchaseCost;

    private LocalDate warrantyExpiryDate;

    private String notes;
}
