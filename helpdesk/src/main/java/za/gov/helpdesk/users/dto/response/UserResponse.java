package za.gov.helpdesk.users.dto.response;

import java.time.LocalDateTime;

import za.gov.helpdesk.users.model.User;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private User.Role role;
    private String phone;
    private String timezone;
    private boolean active;
    private LocalDateTime createdAt;
}
