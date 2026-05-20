package za.gov.helpdesk.auditlog.dto.response;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.dto.response.UserResponse;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long   id;
    private String entityType;
    private Long   entityId;
    private Long   actorId;
    private String actorName;
    private String actorRole;
    private String ipAddress;
    private String action;
    private String oldValue;
    private String newValue;
    private String description;
    private LocalDateTime createdAt;
}
