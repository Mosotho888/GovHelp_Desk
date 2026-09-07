package za.gov.helpdesk.asset.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.repository.AssetRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Helper component for querying asset entities with common fallback exceptions. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetQueryHelper {

    private final AssetRepository assetRepository;

    /**
     * Finds an asset by its ID or throws a {@link ResourceNotFoundException} if not found.
     *
     * @param assetId the unique ID of the asset to look up
     * @return the found {@link Asset} entity
     * @throws ResourceNotFoundException if no asset exists with the given ID
     */
    public Asset findOrThrow(final Long assetId) {
        return assetRepository
                .findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));
    }
}
