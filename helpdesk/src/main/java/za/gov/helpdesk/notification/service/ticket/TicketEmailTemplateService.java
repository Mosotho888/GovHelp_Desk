package za.gov.helpdesk.notification.service.ticket;

import za.gov.helpdesk.notification.dto.PasswordResetEmailNotificationMessage;
import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;

public interface TicketEmailTemplateService {

    String ticketCreatedCustomer(TicketEmailNotificationMessage message);
    String ticketCreatedAgent(TicketEmailNotificationMessage message);
    String ticketAssignedCustomer(TicketEmailNotificationMessage message);
    String ticketAssignedAgent(TicketEmailNotificationMessage message);
    String statusChangedCustomer(TicketEmailNotificationMessage message);
    String commentAddedCustomer(TicketEmailNotificationMessage message);
    String commentAddedAgent(TicketEmailNotificationMessage message);
    String ticketClosedCustomer(TicketEmailNotificationMessage message);
}
