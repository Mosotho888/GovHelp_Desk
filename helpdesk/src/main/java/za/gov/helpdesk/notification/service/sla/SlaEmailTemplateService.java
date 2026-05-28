package za.gov.helpdesk.notification.service.sla;

import java.time.LocalDateTime;

public interface SlaEmailTemplateService {

    String slaWarning(String ticketNumber, String subject, String deadlineType, LocalDateTime dueAt, String agentName);

    String slaBreach(String ticketNumber, String subject, String deadlineType, String agentName);
}
