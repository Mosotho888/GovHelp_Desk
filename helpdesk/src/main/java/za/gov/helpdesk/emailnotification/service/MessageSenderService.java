package za.gov.helpdesk.emailnotification.service;

import za.gov.helpdesk.emailnotification.dto.EmailNotificationDTO;

public interface MessageSenderService {
    void sendTechnicianAssignmentMessage(EmailNotificationDTO notification);
    void sendTicketStatusChangeMessage(EmailNotificationDTO notification);
    void sendTicketCommentMessage(EmailNotificationDTO notification);
    void sendTicketCreationMessage(EmailNotificationDTO notification);
}
