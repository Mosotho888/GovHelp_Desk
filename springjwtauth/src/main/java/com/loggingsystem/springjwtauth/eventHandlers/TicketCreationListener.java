package com.loggingsystem.springjwtauth.eventHandlers;

import com.loggingsystem.springjwtauth.common.util.EmployeeUtil;
import com.loggingsystem.springjwtauth.common.util.TicketUtil;
import com.loggingsystem.springjwtauth.config.messaging.RabbitMQProperties;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.model.EmailNotification;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.emailnotification.repository.EmailNotificationRepository;
import com.loggingsystem.springjwtauth.employee.service.impl.EmployeesServicesImpl;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class TicketCreationListener {
    private final EmailNotificationRepository emailNotificationRepository;
    private final TicketUtil ticketUtil;
    private final EmployeeUtil employeeUtil;
    private final JavaMailSender mailSender;
    private final RabbitMQProperties rabbitMQProperties;

    public TicketCreationListener(EmailNotificationRepository emailNotificationRepository, TicketUtil ticketUtil, EmployeeUtil employeeUtil, JavaMailSender mailSender, RabbitMQProperties rabbitMQProperties) {
        this.emailNotificationRepository = emailNotificationRepository;
        this.ticketUtil = ticketUtil;
        this.employeeUtil = employeeUtil;
        this.mailSender = mailSender;
        this.rabbitMQProperties = rabbitMQProperties;
    }

    @RabbitListener(queues = "ticket_creation_queue")
    public void handleTicketCreationMessage(EmailNotificationDTO request) {

        Tickets ticket = ticketUtil.getTicket(request.getTicketId());
        Employees employee = employeeUtil.getEmployeeByEmail(request.getNormalUserEmail());

        // Create EmailNotification object
        EmailNotification notification = createEmailNotification(request, ticket, employee);

        // Save the notification to the database
        emailNotificationRepository.save(notification);

        try {
            sendEmail(notification);

            notification.setStatus(EmailNotification.EmailStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (MailException e) {
            log.error("Failed to send email for ticket #{}: {}", request.getTicketId(), e.getMessage(), e);
            notification.setStatus(EmailNotification.EmailStatus.FAILED);
            // Consider adding retry logic here if appropriate

        } catch (Exception e) {
            log.error("Error processing ticket creation message for ticket #{}", request.getTicketId(), e);
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
        mailMessage.setFrom("tebohogivenmofokeng@gmail.com");

        mailSender.send(mailMessage);
    }

    @NotNull
    private static EmailNotification createEmailNotification(EmailNotificationDTO request, Tickets ticket, Employees employee) {
        EmailNotification notification = new EmailNotification();
        notification.setTicket(ticket);
        notification.setRecipient(request.getNormalUserEmail());
        notification.setSubject("Your Helpdesk Ticket Has Been Created");
        notification.setBody(getEmailBody(request, employee));
        return notification;
    }

    @NotNull
    private static String getEmailBody(EmailNotificationDTO request, Employees employee) {
        return String.format("""
                        Dear %s %s,

                        Your helpdesk ticket has been successfully created.
                        
                        Ticket Details:
                        ------------------
                        Ticket ID: #%d
                        Issue: %s
                        Priority: %s
                        Category: %s
                        
                        Created by: %s %s
                        Creation Date: %s

                        You can view the status and updates here: [link to ticket details].

                        Best Regards,
                        Support Team""",
                employee.getFirstName(), employee.getLastName(), request.getTicketId(), request.getIssueDescription(),
                request.getPriority(), request.getCategory(), request.getTechnicianName(), request.getTechnicianSurname(),
                request.getCreatedAt()
        );
    }
}
