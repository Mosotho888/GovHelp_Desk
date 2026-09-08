package za.gov.helpdesk.asset.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.dto.response.AssetTicketHistoryResponse;
import za.gov.helpdesk.asset.dto.response.TicketAssetLinkResponse;
import za.gov.helpdesk.users.model.User;

public interface TicketAssetLinkService {

    TicketAssetLinkResponse linkAssetToTicket(Long ticketId, Long assetId, User actor);

    void unlinkAssetFromTicket(Long ticketId, Long assetId, User actor);

    /** Assets currently linked to a ticket - what a technician sees on the ticket's detail view. */
    List<AssetResponse> getAssetsForTicket(Long ticketId, User actor);

    /** An asset's device history: every ticket ever linked to it, most recent first. */
    Page<AssetTicketHistoryResponse> getTicketHistoryForAsset(Long assetId, Pageable pageable);
}
