package za.gov.helpdesk.notification.service.ticket;

import za.gov.helpdesk.notification.dto.TicketEmailNotificationMessage;

public interface TicketEmailService {

    void sendTicketCreated(TicketEmailNotificationMessage message);

    void sendTicketAssigned(TicketEmailNotificationMessage message);

    void sendStatusChanged(TicketEmailNotificationMessage message);

    void sendCommentAdded(TicketEmailNotificationMessage message);

    void sendTicketClosed(TicketEmailNotificationMessage message);
}
