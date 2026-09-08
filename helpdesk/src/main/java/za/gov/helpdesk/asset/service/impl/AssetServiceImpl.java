package za.gov.helpdesk.asset.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.asset.dto.request.CreateAssetRequest;
import za.gov.helpdesk.asset.dto.request.UpdateAssetRequest;
import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.mapper.AssetMapper;
import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;
import za.gov.helpdesk.asset.repository.AssetRepository;
import za.gov.helpdesk.asset.service.AssetAuditService;
import za.gov.helpdesk.asset.service.AssetQueryHelper;
import za.gov.helpdesk.asset.service.AssetService;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.service.UserQueryHelper;

import lombok.RequiredArgsConstructor;

/** Implementation of {@link AssetService} for managing IT asset lifecycles and auditing. */
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetQueryHelper assetQuery;
    private final UserQueryHelper userQuery;
    private final AssetMapper assetMapper;
    private final AssetAuditService assetAuditService;

    @Override
    @Transactional
    public AssetResponse createAsset(final CreateAssetRequest request, final User actor) {
        if (request.getSerialNumber() != null
                && assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new DuplicateResourceException(
                    "An asset with serial number '"
                            + request.getSerialNumber()
                            + "' already exists");
        }
        if (request.getAssetTag() != null
                && assetRepository.existsByAssetTag(request.getAssetTag())) {
            throw new DuplicateResourceException(
                    "Asset tag '" + request.getAssetTag() + "' is already in use");
        }

        final Asset.AssetBuilder builder =
                Asset.builder()
                        .name(request.getName())
                        .type(request.getType())
                        .serialNumber(request.getSerialNumber())
                        .manufacturer(request.getManufacturer())
                        .model(request.getModel())
                        .location(request.getLocation())
                        .vendor(request.getVendor())
                        .purchaseDate(request.getPurchaseDate())
                        .purchaseCost(request.getPurchaseCost())
                        .warrantyExpiryDate(request.getWarrantyExpiryDate())
                        .notes(request.getNotes())
                        .assetTag(
                                request.getAssetTag() != null ? request.getAssetTag() : "PENDING");

        if (request.getAssignedUserId() != null) {
            builder.assignedUser(userQuery.findOrThrow(request.getAssignedUserId()));
        }

        Asset asset = builder.build();
        try {
            asset = assetRepository.save(asset);
        } catch (final DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "An asset with the same tag or serial number already exists", ex);
        }

        if (request.getAssetTag() == null) {
            asset.setAssetTag(generateAssetTag(asset.getId()));
            asset = assetRepository.save(asset);
        }

        assetAuditService.assetCreated(asset, actor);

        return assetMapper.toAssetResponse(asset);
    }

    private String generateAssetTag(final Long id) {
        return "AST-" + String.format("%06d", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(final Long assetId) {
        return assetMapper.toAssetResponse(assetQuery.findOrThrow(assetId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAssets(
            final AssetType type,
            final AssetStatus status,
            final Long assignedUserId,
            final Pageable pageable) {
        return assetRepository
                .findWithFilters(type, status, assignedUserId, pageable)
                .map(assetMapper::toAssetResponse);
    }

    @Override
    @Transactional
    public AssetResponse updateAsset(
            final Long assetId, final UpdateAssetRequest request, final User actor) {
        final Asset asset = assetQuery.findOrThrow(assetId);

        applyStatusChange(asset, request, actor);
        applyAssignmentChange(asset, request, actor);
        applyFieldUpdates(asset, request, actor);

        return assetMapper.toAssetResponse(assetRepository.save(asset));
    }

    private void applyStatusChange(
            final Asset asset, final UpdateAssetRequest request, final User actor) {
        if (request.getStatus() == null || request.getStatus() == asset.getStatus()) {
            return;
        }
        assetAuditService.statusChanged(asset, actor, asset.getStatus(), request.getStatus());

        asset.setStatus(request.getStatus());
    }

    private void applyAssignmentChange(
            final Asset asset, final UpdateAssetRequest request, final User actor) {
        final String previousOwner =
                asset.getAssignedUser() != null ? asset.getAssignedUser().getName() : "Unassigned";

        if (request.isClearAssignedUser() && asset.getAssignedUser() != null) {
            assetAuditService.assignmentChanged(asset, actor, previousOwner, "Unassigned");

            asset.setAssignedUser(null);
            return;
        }

        if (!request.isClearAssignedUser() && request.getAssignedUserId() != null) {
            final boolean alreadyAssignedToThisUser =
                    asset.getAssignedUser() != null
                            && asset.getAssignedUser().getId().equals(request.getAssignedUserId());
            if (alreadyAssignedToThisUser) {
                return;
            }
            final User newOwner = userQuery.findOrThrow(request.getAssignedUserId());
            assetAuditService.assignmentChanged(asset, actor, previousOwner, newOwner.getName());
            asset.setAssignedUser(newOwner);
        }
    }

    private void applyFieldUpdates(
            final Asset asset, final UpdateAssetRequest request, final User actor) {
        final StringBuilder changedFields = new StringBuilder();

        updateIdentityFields(asset, request, changedFields);
        updateHardwareInfo(asset, request, changedFields);
        updateFinancialAndVendorInfo(asset, request, changedFields);

        if (changedFields.length() > 0) {
            final String summary = changedFields.substring(0, changedFields.length() - 2);
            assetAuditService.assetUpdated(asset, actor, "Updated: " + summary);
        }
    }

    private void updateIdentityFields(
            final Asset asset,
            final UpdateAssetRequest request,
            final StringBuilder changedFields) {
        if (request.getName() != null && !request.getName().equals(asset.getName())) {
            asset.setName(request.getName());
            changedFields.append("name, ");
        }
        if (request.getType() != null && request.getType() != asset.getType()) {
            asset.setType(request.getType());
            changedFields.append("type, ");
        }
        if (request.getSerialNumber() != null
                && !request.getSerialNumber().equals(asset.getSerialNumber())) {
            if (assetRepository.existsBySerialNumber(request.getSerialNumber())) {
                throw new DuplicateResourceException(
                        "An asset with serial number '"
                                + request.getSerialNumber()
                                + "' already exists");
            }
            asset.setSerialNumber(request.getSerialNumber());
            changedFields.append("serialNumber, ");
        }
    }

    private void updateHardwareInfo(
            final Asset asset,
            final UpdateAssetRequest request,
            final StringBuilder changedFields) {
        if (request.getManufacturer() != null) {
            asset.setManufacturer(request.getManufacturer());
            changedFields.append("manufacturer, ");
        }
        if (request.getModel() != null) {
            asset.setModel(request.getModel());
            changedFields.append("model, ");
        }
        if (request.getLocation() != null) {
            asset.setLocation(request.getLocation());
            changedFields.append("location, ");
        }
    }

    private void updateFinancialAndVendorInfo(
            final Asset asset,
            final UpdateAssetRequest request,
            final StringBuilder changedFields) {
        if (request.getVendor() != null) {
            asset.setVendor(request.getVendor());
            changedFields.append("vendor, ");
        }
        if (request.getPurchaseDate() != null) {
            asset.setPurchaseDate(request.getPurchaseDate());
            changedFields.append("purchaseDate, ");
        }
        if (request.getPurchaseCost() != null) {
            asset.setPurchaseCost(request.getPurchaseCost());
            changedFields.append("purchaseCost, ");
        }
        if (request.getWarrantyExpiryDate() != null) {
            asset.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
            changedFields.append("warrantyExpiryDate, ");
        }
        if (request.getNotes() != null) {
            asset.setNotes(request.getNotes());
            changedFields.append("notes, ");
        }
    }

    @Override
    @Transactional
    public void retireAsset(final Long assetId, final User actor) {
        final Asset asset = assetQuery.findOrThrow(assetId);
        if (asset.getStatus() == AssetStatus.RETIRED) {
            return;
        }

        final String previousStatus = asset.getStatus().name();
        asset.setStatus(AssetStatus.RETIRED);

        assetAuditService.assetRetired(asset, actor, previousStatus);

        assetRepository.save(asset);
    }
}
