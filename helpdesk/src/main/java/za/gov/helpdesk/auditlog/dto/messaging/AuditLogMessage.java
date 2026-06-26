package za.gov.helpdesk.auditlog.dto.messaging;

import za.gov.helpdesk.auditlog.model.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogMessage {
    private AuditLog.EntityType entityType;
    private Long entityId;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String ipAddress;
    private AuditLog.AuditAction action;
    private String oldValue;
    private String newValue;
    private String description;
    private boolean isAuthLog;
}
