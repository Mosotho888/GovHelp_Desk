package za.gov.helpdesk.auditlog.dto;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.dto.UserResponse;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private Long ticketId;
    private UserResponse actor;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

}
