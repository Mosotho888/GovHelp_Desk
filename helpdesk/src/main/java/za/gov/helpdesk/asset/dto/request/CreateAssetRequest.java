package za.gov.helpdesk.asset.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.asset.model.AssetType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetRequest {

    /** Optional - auto-generated from the asset's id if left blank. */
    @Size(max = 50, message = "Asset tag must not exceed 50 characters")
    private String assetTag;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Asset type is required")
    private AssetType type;

    @Size(max = 100)
    private String serialNumber;

    @Size(max = 100)
    private String manufacturer;

    @Size(max = 100)
    private String model;

    private Long assignedUserId;

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
