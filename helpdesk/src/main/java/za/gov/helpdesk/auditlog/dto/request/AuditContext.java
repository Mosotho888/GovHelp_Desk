package za.gov.helpdesk.auditlog.dto.request;

import za.gov.helpdesk.auditlog.model.AuditLog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditContext {

    AuditLog.EntityType entityType;
    Long entityId;
    Long actorId;
    String actorName;
    String actorRole;
    String ipAddress;
    AuditLog.AuditAction action;
    String oldValue;
    String newValue;
    String description;
}
