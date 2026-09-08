package za.gov.helpdesk.asset.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.asset.dto.request.CreateAssetRequest;
import za.gov.helpdesk.asset.dto.request.UpdateAssetRequest;
import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.dto.response.AssetTicketHistoryResponse;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;
import za.gov.helpdesk.asset.service.AssetService;
import za.gov.helpdesk.asset.service.TicketAssetLinkService;
import za.gov.helpdesk.users.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/assets")
@RequiredArgsConstructor
@Tag(
        name = "Assets",
        description = "IT asset inventory: laptops, desktops, printers, licenses, etc.")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService assetService;
    private final TicketAssetLinkService ticketAssetLinkService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new asset (Admin only)")
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody final CreateAssetRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assetService.createAsset(request, principal.getUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "List assets, optionally filtered by type, status, or assigned user")
    public ResponseEntity<Page<AssetResponse>> getAssets(
            @RequestParam(required = false) final AssetType type,
            @RequestParam(required = false) final AssetStatus status,
            @RequestParam(required = false) final Long assignedUserId,
            @PageableDefault(size = 25, sort = "id") final Pageable pageable) {
        return ResponseEntity.ok(assetService.getAssets(type, status, assignedUserId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get an asset by ID")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable final Long id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Update an asset's details, status, or assignment")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateAssetRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        return ResponseEntity.ok(assetService.updateAsset(id, request, principal.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire an asset (Admin only). History and records are preserved.")
    public void retireAsset(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        assetService.retireAsset(id, principal.getUser());
    }

    @GetMapping("/{id}/tickets")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get an asset's device history: every ticket ever linked to it")
    public ResponseEntity<Page<AssetTicketHistoryResponse>> getAssetTicketHistory(
            @PathVariable final Long id, @PageableDefault(size = 25) final Pageable pageable) {
        return ResponseEntity.ok(ticketAssetLinkService.getTicketHistoryForAsset(id, pageable));
    }
}
