package za.gov.helpdesk.notification.service.auth;

import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;

public interface AuthEmailTemplateService {

    String passwordResetOtp(PasswordResetEmailNotificationMessage message);
}
