package za.gov.helpdesk.auditlog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.users.model.User;

import java.util.List;

public interface AuditService {

    void log (AuditLog.EntityType entityType, Long entityId, User actor, AuditLog.AuditAction action,
              String oldValue, String newValue, String description);

    void log (AuditLog.EntityType entityType, Long entityId, User actor, AuditLog.AuditAction action, String description);

    void logAuth(AuditLog.AuditAction action, Long actorId, String actorName, String actorRole, String description);

    List<AuditLogResponse> getLogsForEntity(AuditLog.EntityType entityType, Long entityId);

    Page<AuditLogResponse> getLogsByActor(Long actorId, Pageable pageable);

    Page<AuditLogResponse> getAuthLogs(Pageable pageable);

    Page<AuditLogResponse> getLogsByAction(AuditLog.AuditAction action, Pageable pageable);
}
