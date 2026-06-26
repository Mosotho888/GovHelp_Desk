package za.gov.helpdesk.notification.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
