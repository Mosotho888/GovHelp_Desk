package za.gov.helpdesk.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private za.gov.helpdesk.users.model.User.Role role;
    private String phone;
    private String timezone;
    private boolean active;
    private LocalDateTime createdAt;
}
