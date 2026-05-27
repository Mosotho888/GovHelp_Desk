package za.gov.helpdesk.notification.service.ticket.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;
import za.gov.helpdesk.notification.service.ticket.TicketEmailTemplateService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEmailServiceImpl implements TicketEmailService {

    private final TicketEmailTemplateService templates;
    private final MailSenderHelper mailer;

    @Override
    public void sendTicketCreated(TicketEmailNotificationMessage message) {
        mailer.send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Created — " + message.getTicketSubject(),
                templates.ticketCreatedCustomer(message));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[New Ticket] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.ticketCreatedAgent(message));
        }
    }

    @Override
    public void sendTicketAssigned(TicketEmailNotificationMessage message) {
        mailer.send(message.getCustomerEmail(),
                "Your Ticket " + message.getTicketNumber() + " Has Been Assigned",
                templates.ticketAssignedCustomer(message));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[Assigned to You] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.ticketAssignedAgent(message));
        }
    }

    @Override
    public void sendStatusChanged(TicketEmailNotificationMessage message) {
        mailer.send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Status Updated to " + message.getTicketStatus(),
                templates.statusChangedCustomer(message));
    }

    @Override
    public void sendCommentAdded(TicketEmailNotificationMessage message) {
        mailer.send(message.getCustomerEmail(),
                "New Reply on Ticket " + message.getTicketNumber(),
                templates.commentAddedCustomer(message));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[Customer Reply] " + message.getTicketNumber() + " — " + message.getTicketSubject(),
                    templates.commentAddedAgent(message));
        }
    }

    @Override
    public void sendTicketClosed(TicketEmailNotificationMessage message) {
        mailer.send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Has Been Closed",
                templates.ticketClosedCustomer(message));
    }
}
