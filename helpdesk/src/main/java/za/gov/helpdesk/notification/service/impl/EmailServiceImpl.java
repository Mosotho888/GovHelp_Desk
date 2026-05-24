package za.gov.helpdesk.notification.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.EmailNotificationMessage;
import za.gov.helpdesk.notification.service.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private EmailTemplateServiceImpl templates;
    private JavaMailSender mailSender;

    @Override
    public void sendTicketCreated(EmailNotificationMessage message) {
        send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Created — " + message.getTicketSubject(),
                templates.ticketCreatedCustomer(message));

        if (message.getAgentEmail() != null) {
            send(message.getAgentEmail(),
                    "[New Ticket] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.ticketCreatedAgent(message));
        }
    }

    @Override
    public void sendTicketAssigned(EmailNotificationMessage message) {
        send(message.getCustomerEmail(),
                "Your Ticket " + message.getTicketNumber() + " Has Been Assigned",
                templates.ticketAssignedCustomer(message));

        if (message.getAgentEmail() != null) {
            send(message.getAgentEmail(),
                    "[Assigned to You] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.ticketAssignedAgent(message));
        }
    }

    @Override
    public void sendStatusChanged(EmailNotificationMessage message) {
        send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Status Updated to " + message.getTicketStatus(),
                templates.statusChangedCustomer(message));
    }

    @Override
    public void sendCommentAdded(EmailNotificationMessage message) {
        send(message.getCustomerEmail(),
                "New Reply on Ticket " + message.getTicketNumber(),
                templates.commentAddedCustomer(message));

        if (message.getAgentEmail() != null) {
            send(message.getAgentEmail(),
                    "[Customer Reply] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.commentAddedAgent(message));
        }
    }

    @Override
    public void sendTicketClosed(EmailNotificationMessage message) {
        send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Has Been Closed",
                templates.ticketClosedCustomer(message));
    }

    private void send(String to, String subject, String html) {
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
