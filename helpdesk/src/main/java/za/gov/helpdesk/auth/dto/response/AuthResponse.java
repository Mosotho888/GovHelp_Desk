package za.gov.helpdesk.auth.dto.response;

import za.gov.helpdesk.users.dto.response.UserResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponse user;
}
