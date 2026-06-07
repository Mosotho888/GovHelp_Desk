package za.gov.helpdesk.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaEmailNotificationMessage {

    private String agentEmail;
    private String agentName;
    private Long ticketId;
    private String ticketNumber;
    private String ticketSubject;
    private String deadlineType;
    private LocalDateTime dueAt;
    private boolean isWarning;
}
