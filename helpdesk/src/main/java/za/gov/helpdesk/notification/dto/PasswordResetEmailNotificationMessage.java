package za.gov.helpdesk.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEmailNotificationMessage {
    private String email;
    private String actorName;
    private String otp;
    private long optExpiryMin;
}
