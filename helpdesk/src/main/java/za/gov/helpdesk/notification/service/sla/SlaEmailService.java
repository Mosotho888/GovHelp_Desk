package za.gov.helpdesk.notification.service.sla;

import java.time.LocalDateTime;

public interface SlaEmailService {

    void sendSlaWarning(
            String to,
            String agentName,
            String ticketNumber,
            String subject,
            String deadlineType,
            LocalDateTime dueAt);

    void sendSlaBreach(
            String to, String agentName, String ticketNumber, String subject, String deadlineType);
}
