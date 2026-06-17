package za.gov.helpdesk.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import za.gov.helpdesk.users.model.User;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Password must contain at least one uppercase letter and one number")
    private String password;

    private User.Role role = User.Role.USER;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be in R.164 format")
    private String phone;

    private String timezone = "Africa/Johannesburg";
}
