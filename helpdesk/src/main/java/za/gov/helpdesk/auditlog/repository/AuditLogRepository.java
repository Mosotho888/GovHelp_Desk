package za.gov.helpdesk.auditlog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.auditlog.model.AuditLog;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {

    List<AuditLog> findByTicketIdOrderByCreatedDateDesc(Long ticketId);

    Page<AuditLog> findByTicketId(Long ticketId, Pageable pageable);
}
