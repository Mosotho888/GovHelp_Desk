package za.gov.helpdesk.auditlog.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.auditlog.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(AuditLog.EntityType entityType, Long entityId);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(AuditLog.EntityType entityType, Pageable pageable);
}
