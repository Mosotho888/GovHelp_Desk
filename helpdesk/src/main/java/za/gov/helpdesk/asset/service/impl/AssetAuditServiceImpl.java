package za.gov.helpdesk.asset.service.impl;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.service.AssetAuditService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/** Handles audit events for IT asset lifecycle operations. */
@Service
@RequiredArgsConstructor
public class AssetAuditServiceImpl implements AssetAuditService {

    private final AuditEventPublisher auditPublisher;

    /**
     * Publishes an audit event when an asset is created.
     *
     * @param asset the created asset
     * @param actor the user who created the asset
     */
    @Override
    public void assetCreated(final Asset asset, final User actor) {
        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                asset.getId(),
                actor,
                AuditLog.AuditAction.ASSET_CREATED,
                null,
                asset.getAssetTag(),
                "Registered asset: " + asset.getName());
    }

    /**
     * Publishes an audit event when an asset status changes.
     *
     * @param asset the asset being updated
     * @param actor the user performing the change
     * @param previousStatus the previous status
     * @param newStatus the new status
     */
    @Override
    public void statusChanged(
            final Asset asset,
            final User actor,
            final AssetStatus previousStatus,
            final AssetStatus newStatus) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                asset.getId(),
                actor,
                AuditLog.AuditAction.ASSET_STATUS_CHANGED,
                previousStatus.name(),
                newStatus.name(),
                null);
    }

    /**
     * Publishes an audit event when an asset assignment changes.
     *
     * @param asset the asset being assigned
     * @param actor the user performing the change
     * @param previousOwner the previous owner
     * @param newOwner the new owner
     */
    @Override
    public void assignmentChanged(
            final Asset asset,
            final User actor,
            final String previousOwner,
            final String newOwner) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                asset.getId(),
                actor,
                AuditLog.AuditAction.ASSET_ASSIGNED,
                previousOwner,
                newOwner,
                null);
    }

    /**
     * Publishes an audit event when asset fields are updated.
     *
     * @param asset the updated asset
     * @param actor the user performing the update
     * @param summary summary of the fields that changed
     */
    @Override
    public void assetUpdated(final Asset asset, final User actor, final String summary) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                asset.getId(),
                actor,
                AuditLog.AuditAction.ASSET_UPDATED,
                null,
                null,
                "Updated: " + summary);
    }

    /**
     * Publishes an audit event when an asset is retired.
     *
     * @param asset the retired asset
     * @param actor the user performing the retirement
     * @param previousStatus the status before retirement
     */
    @Override
    public void assetRetired(final Asset asset, final User actor, final String previousStatus) {

        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                asset.getId(),
                actor,
                AuditLog.AuditAction.ASSET_RETIRED,
                previousStatus,
                asset.getStatus().name(),
                null);
    }
}
