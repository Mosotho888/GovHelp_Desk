package za.gov.helpdesk.notification.service;

import za.gov.helpdesk.notification.dto.EmailNotificationMessage;

public interface EmailService {

    void sendTicketCreated(EmailNotificationMessage message);
    void sendTicketAssigned(EmailNotificationMessage message);
    void sendStatusChanged(EmailNotificationMessage message);
    void sendCommentAdded(EmailNotificationMessage message);
    void sendTicketClosed(EmailNotificationMessage message);
}
