package za.gov.helpdesk.asset.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.dto.response.TicketAssetLinkResponse;
import za.gov.helpdesk.asset.service.TicketAssetLinkService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Links tickets to the assets they concern, giving technicians device history at a glance. Kept as
 * a separate controller from {@code TicketController}, mirroring how {@code AttachmentController}
 * owns the nested {@code /v1/tickets/{ticketId}/attachments} paths.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Ticket Assets", description = "Associating tickets with the IT assets they concern")
@SecurityRequirement(name = "bearerAuth")
public class TicketAssetController {

    private final TicketAssetLinkService ticketAssetLinkService;

    /**
     * Links an IT asset to a specific ticket.
     *
     * @param ticketId the ID of the ticket to link
     * @param assetId the ID of the asset to link
     * @param principal the authenticated user performing the action
     * @return the link response containing details of the created association
     */
    @PostMapping("/v1/tickets/{ticketId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Link an asset to a ticket")
    public ResponseEntity<TicketAssetLinkResponse> linkAsset(
            @PathVariable final Long ticketId,
            @PathVariable final Long assetId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ticketAssetLinkService.linkAssetToTicket(
                                ticketId, assetId, principal.getUser()));
    }

    /**
     * Unlinks an IT asset from a ticket.
     *
     * @param ticketId the ID of the ticket
     * @param assetId the ID of the asset to unlink
     * @param principal the authenticated user performing the action
     */
    @DeleteMapping("/v1/tickets/{ticketId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unlink an asset from a ticket")
    public void unlinkAsset(
            @PathVariable final Long ticketId,
            @PathVariable final Long assetId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        ticketAssetLinkService.unlinkAssetFromTicket(ticketId, assetId, principal.getUser());
    }

    /**
     * Retrieves all IT assets currently linked to a specified ticket.
     *
     * @param ticketId the ID of the ticket
     * @param principal the authenticated user making the request
     * @return a list of asset responses associated with the ticket
     */
    @GetMapping("/v1/tickets/{ticketId}/assets")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "List the assets currently linked to a ticket")
    public ResponseEntity<List<AssetResponse>> getAssetsForTicket(
            @PathVariable final Long ticketId,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(
                ticketAssetLinkService.getAssetsForTicket(ticketId, principal.getUser()));
    }
}
