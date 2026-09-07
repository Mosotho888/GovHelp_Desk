package za.gov.helpdesk.asset.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.asset.dto.request.CreateAssetRequest;
import za.gov.helpdesk.asset.dto.request.UpdateAssetRequest;
import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;
import za.gov.helpdesk.users.model.User;

public interface AssetService {

    AssetResponse createAsset(CreateAssetRequest request, User actor);

    AssetResponse getAssetById(Long assetId);

    Page<AssetResponse> getAssets(
            AssetType type, AssetStatus status, Long assignedUserId, Pageable pageable);

    AssetResponse updateAsset(Long assetId, UpdateAssetRequest request, User actor);

    /** Soft-retires an asset (status -> RETIRED); its ticket history and records are preserved. */
    void retireAsset(Long assetId, User actor);
}
