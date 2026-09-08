package za.gov.helpdesk.notification.service.sla;

import za.gov.helpdesk.notification.dto.SlaEmailNotificationMessage;

public interface SlaEmailService {

    void sendSlaWarning(SlaEmailNotificationMessage message);

    void sendSlaBreach(SlaEmailNotificationMessage message);
}
