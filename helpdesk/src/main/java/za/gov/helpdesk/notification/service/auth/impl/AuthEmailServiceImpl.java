package za.gov.helpdesk.notification.service.auth.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.service.EmailTemplateRenderer;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.auth.AuthEmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthEmailServiceImpl implements AuthEmailService {

    private static final String PREFIX = "email/auth/";

    private final EmailTemplateRenderer renderer;
    private final MailSenderHelper mailer;

    @Override
    public void sendPasswordResetOtp(final PasswordResetEmailNotificationMessage message) {
        final Map<String, Object> model =
                Map.of(
                        "actorName",
                        message.getActorName(),
                        "otp",
                        message.getOtp(),
                        "otpExpiryMin",
                        message.getOptExpiryMin() // DTO typo kept as-is
                        );
        mailer.send(
                message.getEmail(),
                "Your Password Reset Code - Government Helpdesk",
                renderer.render(PREFIX + "password-reset-otp", model));
    }
}
