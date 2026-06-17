package za.gov.helpdesk.notification.service.ticket.impl;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;
import za.gov.helpdesk.notification.service.EmailTemplateRenderer;
import za.gov.helpdesk.notification.service.MailSenderHelper;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEmailServiceImpl implements TicketEmailService {

    private static final String PREFIX = "email/ticket/";

    private final EmailTemplateRenderer renderer;
    private final MailSenderHelper mailer;

    @Override
    public void sendTicketCreated(TicketEmailNotificationMessage message) {

        Map<String, Object> model = Map.of("msg", message);

        mailer.send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Created - " + message.getTicketSubject(),
                renderer.render(PREFIX + "ticket-created-customer", model));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[New Ticket] " + message.getTicketNumber() + " - " + message.getTicketSubject(),
                    renderer.render(PREFIX + "ticket-created-agent", model));
        }
    }

    @Override
    public void sendTicketAssigned(TicketEmailNotificationMessage message) {
        Map<String, Object> model = Map.of("msg", message);

        mailer.send(message.getCustomerEmail(), "Your Ticket " + message.getTicketNumber() + " Has Been Assigned",
                renderer.render(PREFIX + "ticket-assigned-customer", model));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[Assigned to You] " + message.getTicketNumber() + " - " + message.getTicketSubject(),
                    renderer.render(PREFIX + "ticket-assigned-agent", model));
        }
    }

    @Override
    public void sendStatusChanged(TicketEmailNotificationMessage message) {
        Map<String, Object> model = Map.of("msg", message);

        mailer.send(message.getCustomerEmail(),
                "Ticket " + message.getTicketNumber() + " Status Updated to " + message.getTicketStatus(),
                renderer.render(PREFIX + "status-changed-customer", model));
    }

    @Override
    public void sendCommentAdded(TicketEmailNotificationMessage message) {
        Map<String, Object> model = Map.of("msg", message);

        mailer.send(message.getCustomerEmail(), "New Reply on Ticket " + message.getTicketNumber(),
                renderer.render(PREFIX + "comment-added-customer", model));

        if (message.getAgentEmail() != null) {
            mailer.send(message.getAgentEmail(),
                    "[Customer Reply] " + message.getTicketNumber() + " - " + message.getTicketSubject(),
                    renderer.render(PREFIX + "comment-added-agent", model));
        }
    }

    @Override
    public void sendTicketClosed(TicketEmailNotificationMessage message) {
        Map<String, Object> model = Map.of("msg", message);

        mailer.send(message.getCustomerEmail(), "Ticket " + message.getTicketNumber() + " Has Been Closed",
                renderer.render(PREFIX + "ticket-closed-customer", model));
    }
}
