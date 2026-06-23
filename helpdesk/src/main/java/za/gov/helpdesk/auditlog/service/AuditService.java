package za.gov.helpdesk.auditlog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.auditlog.dto.request.AuditContext;
import za.gov.helpdesk.auditlog.dto.request.AuthAuditContext;
import za.gov.helpdesk.auditlog.dto.response.AuditLogResponse;
import za.gov.helpdesk.auditlog.model.AuditLog;

public interface AuditService {

    void log(AuditContext context);

    void logAuth(AuthAuditContext context);

    List<AuditLogResponse> getLogsForEntity(AuditLog.EntityType entityType, Long entityId);

    Page<AuditLogResponse> getLogsByActor(Long actorId, Pageable pageable);

    Page<AuditLogResponse> getAuthLogs(Pageable pageable);

    Page<AuditLogResponse> getLogsByAction(AuditLog.AuditAction action, Pageable pageable);
}
