package za.gov.helpdesk.auditlog.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String ipAddress;
    private String action;
    private String oldValue;
    private String newValue;
    private String description;
    private LocalDateTime createdAt;
}
