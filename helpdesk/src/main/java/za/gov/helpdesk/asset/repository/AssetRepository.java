package za.gov.helpdesk.asset.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.asset.model.Asset;
import za.gov.helpdesk.asset.model.AssetStatus;
import za.gov.helpdesk.asset.model.AssetType;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByAssetTag(String assetTag);

    boolean existsByAssetTag(String assetTag);

    boolean existsBySerialNumber(String serialNumber);

    @Query(
            """
            SELECT a FROM Asset a
            WHERE (:type IS NULL OR a.type = :type)
              AND (:status IS NULL OR a.status = :status)
              AND (:assignedUserId IS NULL OR a.assignedUser.id = :assignedUserId)
            """)
    Page<Asset> findWithFilters(
            @Param("type") AssetType type,
            @Param("status") AssetStatus status,
            @Param("assignedUserId") Long assignedUserId,
            Pageable pageable);
}
