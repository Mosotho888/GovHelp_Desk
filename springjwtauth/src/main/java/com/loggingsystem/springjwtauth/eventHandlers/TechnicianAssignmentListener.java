package com.loggingsystem.springjwtauth.eventHandlers;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.config.messaging.RabbitMQProperties;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.model.EmailNotification;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.emailnotification.repository.EmailNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TechnicianAssignmentListener {
    private final RabbitMQProperties rabbitMQProperties;
    private final EmailNotificationRepository emailNotificationRepository;
    private final TicketUtils ticketUtils;
    private final JavaMailSender mailSender;

    public TechnicianAssignmentListener(RabbitMQProperties rabbitMQProperties, EmailNotificationRepository emailNotificationRepository, TicketUtils ticketUtils, JavaMailSender mailSender) {
        this.rabbitMQProperties = rabbitMQProperties;
        this.emailNotificationRepository = emailNotificationRepository;
        this.ticketUtils = ticketUtils;
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "technician_assignment_queue")
    public void handleTechnicianAssignmentMessage(EmailNotificationDTO request) {
        Tickets ticket = ticketUtils.getTicket(request.getTicketId());

        EmailNotification notification = createEmailNotification(request, ticket);

        emailNotificationRepository.save(notification);

        try {
            sendEmail(notification);

            log.info("Email notification sent for ticket #{}", request.getTicketId());
            notification.setStatus(EmailNotification.EmailStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (MailException ex) {
            log.error("Failed to send email notification for ticket #{}: {}", request.getTicketId(), ex.getMessage(), ex);
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            // Adding retry logic here

        } catch (Exception e) {
            log.error("Error processing ticket assignment message for ticket #{}", request.getTicketId(), e);
            // Consider adding retry logic here if appropriate

        }finally {
            emailNotificationRepository.save(notification);
        }

    }

    private void sendEmail(EmailNotification notification) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo("tmofokeng@moseskotane.gov.za");
        mailMessage.setSubject(notification.getSubject());
        mailMessage.setText(notification.getBody());
        mailMessage.setFrom("testmessagespring@gmail.com");

        mailSender.send(mailMessage);
    }

    @NotNull
    private static EmailNotification createEmailNotification(EmailNotificationDTO request, Tickets ticket) {
        EmailNotification notification = new EmailNotification();
        notification.setTicket(ticket);
        notification.setRecipient(request.getTechnicianEmail());
        notification.setSubject("New Ticket Assigned: #" + request.getTicketId());
        notification.setBody(getEmailBody(request));
        return notification;
    }

    @NotNull
    private static String getEmailBody(EmailNotificationDTO request) {
        return String.format("""
                        Dear %s %s,

                        You have been assigned a new support ticket #%d.

                        Ticket Details:
                        ------------------
                        Ticket ID: %d
                        Issue: %s
                        Priority: %s
                        Category: %s
                        
                        Due Date: %s

                        Please check your dashboard for more details.

                        Best Regards,
                        Support Team""",
                request.getTechnicianName(), request.getTechnicianSurname(), request.getTicketId(),
                request.getTicketId(), request.getIssueDescription(), request.getPriority(),
                request.getCategory(), request.getDueAt()
        );
    }
}
