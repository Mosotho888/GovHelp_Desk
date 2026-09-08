package za.gov.helpdesk.asset.service;

import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.users.model.User;

/** Handles audit events for asset lifecycle operations. */
public interface AssetAuditService {

    void assetCreated(Asset asset, User actor);

    void statusChanged(Asset asset, User actor, AssetStatus previousStatus, AssetStatus newStatus);

    void assignmentChanged(Asset asset, User actor, String previousOwner, String newOwner);

    void assetUpdated(Asset asset, User actor, String summary);

    void assetRetired(Asset asset, User actor, String previousStatus);
}
