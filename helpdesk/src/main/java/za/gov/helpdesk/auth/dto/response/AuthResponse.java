package za.gov.helpdesk.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.users.dto.response.UserResponse;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponse user;
}
