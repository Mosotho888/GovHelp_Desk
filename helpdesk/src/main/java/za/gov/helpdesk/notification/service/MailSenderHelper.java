package za.gov.helpdesk.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailSenderHelper {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Email sent: to={} subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email: to={} subject={} error={}", to, subject, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }
}
