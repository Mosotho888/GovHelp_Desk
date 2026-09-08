package za.gov.helpdesk.asset.mapper;

import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.users.mapper.UserMapper;

/** Mapper interface for converting {@link Asset} domain entities into DTO responses. */
@Mapper(componentModel = "spring", uses = UserMapper.class)
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AssetMapper {

    /** Warranties inside this window are flagged EXPIRING_SOON rather than plain ACTIVE. */
    int EXPIRING_SOON_THRESHOLD_DAYS = 30;

    /**
     * Maps an {@link Asset} domain entity to its corresponding {@link AssetResponse} DTO.
     *
     * @param asset the asset entity to convert
     * @return the mapped asset response DTO
     */
    @Mapping(
            target = "warrantyStatus",
            source = "warrantyExpiryDate",
            qualifiedByName = "toWarrantyStatus")
    AssetResponse toAssetResponse(Asset asset);

    /**
     * Calculates the warranty status based on the warranty expiry date relative to today.
     *
     * @param warrantyExpiryDate the date when the warranty expires
     * @return the corresponding {@link AssetResponse.WarrantyStatus}
     */
    @Named("toWarrantyStatus")
    default AssetResponse.WarrantyStatus toWarrantyStatus(final LocalDate warrantyExpiryDate) {
        if (warrantyExpiryDate == null) {
            return AssetResponse.WarrantyStatus.NO_WARRANTY_INFO;
        }
        final LocalDate today = LocalDate.now();
        if (warrantyExpiryDate.isBefore(today)) {
            return AssetResponse.WarrantyStatus.EXPIRED;
        }
        if (!warrantyExpiryDate.isAfter(today.plusDays(EXPIRING_SOON_THRESHOLD_DAYS))) {
            return AssetResponse.WarrantyStatus.EXPIRING_SOON;
        }
        return AssetResponse.WarrantyStatus.ACTIVE;
    }
}
