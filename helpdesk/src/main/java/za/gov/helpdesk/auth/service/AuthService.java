package za.gov.helpdesk.auth.service;

import za.gov.helpdesk.auth.dto.request.LoginRequest;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.users.model.User;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);
    AuthResponse refresh(RefreshTokenRequest refreshToken);
    void logout(String rawRefreshToken, User actor);
}
