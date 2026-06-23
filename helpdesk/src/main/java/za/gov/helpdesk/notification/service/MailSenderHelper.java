package za.gov.helpdesk.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility helper component that encapsulates low-level Jakarta Mail connection details. Simplifies
 * the construction and transmission of multipart HTML messages by managing explicit MIME structural
 * configuration headers, encoding configurations, and error translation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailSenderHelper {

    private final JavaMailSender mailSender;

    /**
     * Compiles and dispatches a multipart MIME email message using a UTF-8 character charset
     * profile. Instructs the transmission layer to process the message string body as valid inline
     * HTML code.
     *
     * @param to the target destination email address recipient
     * @param subject the descriptive topic header line string for the outbound message
     * @param html the processed string body containing the structural HTML layout content
     * @throws RuntimeException a wrapped unhandled exception container if SMTP transport boundaries
     *     or parsing constraints collapse during delivery execution
     */
    public void send(final String to, final String subject, final String html) {
        try {
            final MimeMessage mime = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Email sent: to={} subject={}", to, subject);
        } catch (final MessagingException e) {
            log.error(
                    "Failed to send email: to={} subject={} error={}", to, subject, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }
}
