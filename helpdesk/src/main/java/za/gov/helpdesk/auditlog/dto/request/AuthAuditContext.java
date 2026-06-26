package za.gov.helpdesk.auditlog.dto.request;

import za.gov.helpdesk.auditlog.model.AuditLog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthAuditContext {
    AuditLog.AuditAction action;
    Long actorId;
    String actorName;
    String actorRole;
    String ipAddress;
    String description;
}
