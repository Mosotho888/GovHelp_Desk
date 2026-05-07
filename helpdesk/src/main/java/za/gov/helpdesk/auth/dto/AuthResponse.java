package za.gov.helpdesk.auth.dto;

import lombok.Builder;
import lombok.Data;
import za.gov.helpdesk.employee.dto.EmployeeResponse;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private EmployeeResponse user;
}

