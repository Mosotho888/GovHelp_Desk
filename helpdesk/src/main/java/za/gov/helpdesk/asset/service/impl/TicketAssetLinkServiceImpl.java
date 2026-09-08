package za.gov.helpdesk.asset.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.asset.dto.response.AssetResponse;
import za.gov.helpdesk.asset.dto.response.AssetTicketHistoryResponse;
import za.gov.helpdesk.asset.dto.response.TicketAssetLinkResponse;
import za.gov.helpdesk.asset.mapper.AssetMapper;
import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.model.TicketAsset;
import za.gov.helpdesk.asset.repository.TicketAssetRepository;
import za.gov.helpdesk.asset.service.AssetQueryHelper;
import za.gov.helpdesk.asset.service.TicketAssetLinkService;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.exception.DuplicateResourceException;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.service.TicketQueryHelper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketAssetLinkServiceImpl implements TicketAssetLinkService {

    private final TicketAssetRepository ticketAssetRepository;
    private final TicketQueryHelper ticketQuery;
    private final AssetQueryHelper assetQuery;
    private final AssetMapper assetMapper;
    private final AuditEventPublisher auditPublisher;

    @Override
    @Transactional
    public TicketAssetLinkResponse linkAssetToTicket(
            final Long ticketId, final Long assetId, final User actor) {
        final Ticket ticket = ticketQuery.findOrThrow(ticketId, actor);
        final Asset asset = assetQuery.findOrThrow(assetId);

        if (ticketAssetRepository.existsByTicketIdAndAssetId(ticketId, assetId)) {
            throw new DuplicateResourceException(
                    "Asset " + asset.getAssetTag() + " is already linked to this ticket");
        }

        final TicketAsset link =
                ticketAssetRepository.save(
                        TicketAsset.builder()
                                .ticket(ticket)
                                .asset(asset)
                                .linkedById(actor.getId())
                                .linkedByName(actor.getName())
                                .build());

        final String description =
                "Asset "
                        + asset.getAssetTag()
                        + " ("
                        + asset.getName()
                        + ") linked to ticket #"
                        + ticketId;

        // Logged against both entities so the link is discoverable from either the ticket's audit
        // trail or the asset's own history.
        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticketId,
                actor,
                AuditLog.AuditAction.ASSET_LINKED_TO_TICKET,
                null,
                asset.getAssetTag(),
                description);
        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                assetId,
                actor,
                AuditLog.AuditAction.ASSET_LINKED_TO_TICKET,
                null,
                "Ticket #" + ticketId,
                description);

        return TicketAssetLinkResponse.builder()
                .ticketId(ticketId)
                .assetId(assetId)
                .assetTag(asset.getAssetTag())
                .assetName(asset.getName())
                .linkedByName(link.getLinkedByName())
                .linkedAt(link.getLinkedAt())
                .build();
    }

    @Override
    @Transactional
    public void unlinkAssetFromTicket(final Long ticketId, final Long assetId, final User actor) {
        // Confirms the actor may see this ticket at all before revealing/mutating its asset links.
        ticketQuery.findOrThrow(ticketId, actor);

        final TicketAsset link =
                ticketAssetRepository
                        .findByTicketIdAndAssetId(ticketId, assetId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Asset "
                                                        + assetId
                                                        + " is not linked to ticket "
                                                        + ticketId));

        final String assetTag = link.getAsset().getAssetTag();
        ticketAssetRepository.delete(link);

        final String description = "Asset " + assetTag + " unlinked from ticket #" + ticketId;
        auditPublisher.publishAudit(
                AuditLog.EntityType.TICKET,
                ticketId,
                actor,
                AuditLog.AuditAction.ASSET_UNLINKED_FROM_TICKET,
                assetTag,
                null,
                description);
        auditPublisher.publishAudit(
                AuditLog.EntityType.ASSET,
                assetId,
                actor,
                AuditLog.AuditAction.ASSET_UNLINKED_FROM_TICKET,
                "Ticket #" + ticketId,
                null,
                description);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsForTicket(final Long ticketId, final User actor) {
        ticketQuery.findOrThrow(ticketId, actor);

        return ticketAssetRepository.findByTicketIdOrderByLinkedAtDesc(ticketId).stream()
                .map(TicketAsset::getAsset)
                .map(assetMapper::toAssetResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetTicketHistoryResponse> getTicketHistoryForAsset(
            final Long assetId, final Pageable pageable) {
        assetQuery.findOrThrow(assetId);

        return ticketAssetRepository
                .findByAssetIdOrderByLinkedAtDesc(assetId, pageable)
                .map(
                        link -> {
                            final Ticket ticket = link.getTicket();
                            return AssetTicketHistoryResponse.builder()
                                    .ticketId(ticket.getId())
                                    .subject(ticket.getSubject())
                                    .status(ticket.getStatus())
                                    .priority(ticket.getPriority())
                                    .linkedAt(link.getLinkedAt())
                                    .build();
                        });
    }
}
