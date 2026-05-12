//package za.gov.helpdesk.eventHandlers;
//
//import za.gov.helpdesk.common.util.EmployeeUtil;
//import za.gov.helpdesk.common.util.TicketUtil;
//import za.gov.helpdesk.config.messaging.RabbitMQProperties;
//import za.gov.helpdesk.emailnotification.dto.EmailNotificationDTO;
//import za.gov.helpdesk.emailnotification.model.EmailNotification;
//import za.gov.helpdesk.ticket.model.Ticket;
//import za.gov.helpdesk.emailnotification.repository.EmailNotificationRepository;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//@Slf4j
//public class TicketStatusChangeListener {
//    private final EmailNotificationRepository emailNotificationRepository;
//    private final TicketUtil ticketUtil;
//    private final EmployeeUtil employeeUtil;
//    private final JavaMailSender mailSender;
//    private final RabbitMQProperties rabbitMQProperties;
//
//    public TicketStatusChangeListener(EmailNotificationRepository emailNotificationRepository, TicketUtil ticketUtil, EmployeeUtil employeeUtil, JavaMailSender mailSender, RabbitMQProperties rabbitMQProperties) {
//        this.emailNotificationRepository = emailNotificationRepository;
//        this.ticketUtil = ticketUtil;
//        this.employeeUtil = employeeUtil;
//        this.mailSender = mailSender;
//        this.rabbitMQProperties = rabbitMQProperties;
//    }
//
//    @RabbitListener(queues = "ticket_status_change_queue")
//    public void handleTicketStatusChangeMessage(EmailNotificationDTO request) {
//        Ticket ticket = ticketUtil.getTicket(request.getTicketId());
//        za.gov.helpdesk.users.model.User employee = employeeUtil.getEmployeeByEmail(request.getNormalUserEmail());
//
//        EmailNotification notification = createEmailNotification(request, ticket, employee);
//        emailNotificationRepository.save(notification);
//
//        try {
//
//            sendEmail(notification);
//
//            log.info("Email notification sent for Status Change, Ticket #{}", request.getTicketId());
//            notification.setStatus(EmailNotification.EmailStatus.SENT);
//            notification.setSentAt(LocalDateTime.now());
//
//        } catch (EntityNotFoundException e) {
//            log.error("Failed to send email for ticket #{}: {}", request.getTicketId(), e.getMessage(), e);
//            notification.setStatus(EmailNotification.EmailStatus.FAILED);
//            // Consider adding retry logic here if appropriate
//
//        } catch (Exception e) {
//            log.error("Error processing status change message for ticket #{}", request.getTicketId(), e);
//            notification.setStatus(EmailNotification.EmailStatus.FAILED);
//            // Consider adding retry logic here if appropriate
//
//        } finally {
//            emailNotificationRepository.save(notification);
//        }
//    }
//
//    private void sendEmail(EmailNotification notification) {
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//        mailMessage.setTo("tmofokeng@moseskotane.gov.za");
//        mailMessage.setSubject(notification.getSubject());
//        mailMessage.setText(notification.getBody());
//        mailMessage.setFrom("testmessagespring@gmail.com");
//
//        mailSender.send(mailMessage);
//    }
//
//    @NotNull
//    private static EmailNotification createEmailNotification(EmailNotificationDTO request, Ticket ticket, za.gov.helpdesk.users.model.User employee) {
//        EmailNotification notification = new EmailNotification();
//        notification.setTicket(ticket);
//        notification.setRecipient(request.getNormalUserEmail());
//        notification.setSubject("Status Updated For Ticket: #" + request.getTicketId());
//        notification.setBody(getEmailBody(request, employee));
//        return notification;
//    }
//
//    @NotNull
//    private static String getEmailBody(EmailNotificationDTO request, za.gov.helpdesk.users.model.User employee) {
//        return String.format("""
//                        Dear %s %s,
//
//                        Ticket #%d status has been updated.
//
//                        Ticket ID: %d
//                        Status: %s
//
//                        Updated By: %s %s
//                        Update Date: %s
//
//                        Please check your dashboard for more details.
//
//                        Best Regards,
//                        Support Team""",
//                employee.getFirstName(), employee.getLastName(), request.getTicketId(),
//                request.getTicketId(), request.getStatus(), request.getTechnicianName(), request.getTechnicianSurname(),
//                request.getUpdatedAt()
//        );
//    }
//}
