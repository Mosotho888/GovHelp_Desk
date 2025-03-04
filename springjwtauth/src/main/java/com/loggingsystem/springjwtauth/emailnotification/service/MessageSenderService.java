package com.loggingsystem.springjwtauth.emailnotification.service;

import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;

public interface MessageSenderService {
    void sendTechnicianAssignmentMessage(EmailNotificationDTO notification);
    void sendTicketStatusChangeMessage(EmailNotificationDTO notification);
    void sendTicketCommentMessage(EmailNotificationDTO notification);
    void sendTicketCreationMessage(EmailNotificationDTO notification);
}
