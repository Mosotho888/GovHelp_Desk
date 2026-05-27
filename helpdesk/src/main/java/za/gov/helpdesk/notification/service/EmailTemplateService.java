package za.gov.helpdesk.notification.service;

import za.gov.helpdesk.notification.dto.EmailNotificationMessage;

public interface EmailTemplateService {

    String ticketCreatedCustomer(EmailNotificationMessage msg);
    String ticketCreatedAgent(EmailNotificationMessage msg);
    String ticketAssignedCustomer(EmailNotificationMessage msg);
    String ticketAssignedAgent(EmailNotificationMessage msg);
    String statusChangedCustomer(EmailNotificationMessage msg);
    String commentAddedCustomer(EmailNotificationMessage msg);
    String commentAddedAgent(EmailNotificationMessage msg);
    String ticketClosedCustomer(EmailNotificationMessage msg);
    String passwordResetOtp(String name, String otp, long expiryMinutes);
}
