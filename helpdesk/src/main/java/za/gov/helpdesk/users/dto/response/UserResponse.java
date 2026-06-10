package za.gov.helpdesk.users.dto.response;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

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
