package za.gov.helpdesk.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.gov.helpdesk.auditlog.model.AuditLog;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationMessage {

    private AuditLog.AuditAction trigger;

    private Long   ticketId;
    private String ticketNumber;
    private String ticketSubject;
    private String ticketStatus;
    private String ticketPriority;
    private String comment;

    private String customerEmail;
    private String customerName;

    private String agentEmail;
    private String agentName;
}
