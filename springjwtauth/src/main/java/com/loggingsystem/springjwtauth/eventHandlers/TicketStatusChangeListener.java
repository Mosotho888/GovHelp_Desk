package com.loggingsystem.springjwtauth.eventHandlers;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.model.EmailNotification;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.emailnotification.repository.EmailNotificationRepository;
import com.loggingsystem.springjwtauth.employee.service.EmployeesServices;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TicketStatusChangeListener {
    private final EmailNotificationRepository emailNotificationRepository;
    private final TicketUtils ticketUtils;
    private final EmployeesServices employeesService;
    private final JavaMailSender mailSender;

    public TicketStatusChangeListener(EmailNotificationRepository emailNotificationRepository, TicketUtils ticketUtils, EmployeesServices employeesService, JavaMailSender mailSender) {
        this.emailNotificationRepository = emailNotificationRepository;
        this.ticketUtils = ticketUtils;
        this.employeesService = employeesService;
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "#{ticketStatusChangeQueue}")
    public void handleTicketStatusChangeMessage(EmailNotificationDTO request) {
        Tickets ticket = ticketUtils.getTicket(request.getTicketId());
        Employees employee = employeesService.getEmployeeByEmail(request.getNormalUserEmail());

        EmailNotification notification = createEmailNotification(request, ticket, employee);
        emailNotificationRepository.save(notification);

        try {

            sendEmail(notification);

            log.info("Email notification sent for Status Change, Ticket #{}", request.getTicketId());
            notification.setStatus(EmailNotification.EmailStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (EntityNotFoundException e) {
            log.error("Failed to send email for ticket #{}: {}", request.getTicketId(), e.getMessage(), e);
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            // Consider adding retry logic here if appropriate

        } catch (Exception e) {
            log.error("Error processing status change message for ticket #{}", request.getTicketId(), e);
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            // Consider adding retry logic here if appropriate

        } finally {
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
    private static EmailNotification createEmailNotification(EmailNotificationDTO request, Tickets ticket, Employees employee) {
        EmailNotification notification = new EmailNotification();
        notification.setTicket(ticket);
        notification.setRecipient(request.getNormalUserEmail());
        notification.setSubject("Status Updated For Ticket: #" + request.getTicketId());
        notification.setBody(getEmailBody(request, employee));
        return notification;
    }

    @NotNull
    private static String getEmailBody(EmailNotificationDTO request, Employees employee) {
        return String.format("""
                        Dear %s %s,

                        Ticket #%d status has been updated.

                        Ticket ID: %d
                        Status: %s
                        
                        Updated By: %s %s
                        Update Date: %s

                        Please check your dashboard for more details.

                        Best Regards,
                        Support Team""",
                employee.getFirst_name(), employee.getLast_name(), request.getTicketId(),
                request.getTicketId(), request.getStatus(), request.getTechnicianName(), request.getTechnicianSurname(),
                request.getUpdatedAt()
        );
    }
}
