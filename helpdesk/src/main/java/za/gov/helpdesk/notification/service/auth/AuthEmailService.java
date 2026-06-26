package za.gov.helpdesk.notification.service.auth;

import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;

public interface AuthEmailService {

    void sendPasswordResetOtp(PasswordResetEmailNotificationMessage msg);
}
