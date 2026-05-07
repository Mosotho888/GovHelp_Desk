package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.auth.dto.LoginRequest;
import za.gov.helpdesk.auth.dto.AuthResponse;
import za.gov.helpdesk.auth.dto.RefreshTokenRequest;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refresh(RefreshTokenRequest refreshToken);
}
