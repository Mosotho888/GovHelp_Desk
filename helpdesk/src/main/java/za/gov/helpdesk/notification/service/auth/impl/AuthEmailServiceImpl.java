package za.gov.helpdesk.notification.service.auth.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.auth.AuthEmailService;
import za.gov.helpdesk.notification.service.auth.AuthEmailTemplateService;

@Service
@RequiredArgsConstructor
public class AuthEmailServiceImpl implements AuthEmailService {

    private final AuthEmailTemplateService templates;
    private final MailSenderHelper mailer;

    @Override
    public void sendPasswordResetOtp(PasswordResetEmailNotificationMessage message) {
        mailer.send(
                message.getEmail(),
                "Your Password Reset Code - Government Helpdesk",
                templates.passwordResetOtp(message)
        );
    }
}
