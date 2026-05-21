package za.gov.helpdesk.auditlog.dto.messaging;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.gov.helpdesk.auditlog.model.AuditLog;

@Data
@Builder
public class AuditLogMessage {
    private AuditLog.EntityType entityType;
    private Long entityId;
    private Long actorId;
    private AuditLog.AuditAction action;
    private String oldValue;
    private String newValue;
    private String description;
    private boolean isAuthLog;

}
