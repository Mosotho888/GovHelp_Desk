package za.gov.helpdesk.asset.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.asset.model.TicketAsset;

@Repository
public interface TicketAssetRepository extends JpaRepository<TicketAsset, Long> {

    List<TicketAsset> findByTicketIdOrderByLinkedAtDesc(Long ticketId);

    Page<TicketAsset> findByAssetIdOrderByLinkedAtDesc(Long assetId, Pageable pageable);

    Optional<TicketAsset> findByTicketIdAndAssetId(Long ticketId, Long assetId);

    boolean existsByTicketIdAndAssetId(Long ticketId, Long assetId);
}
