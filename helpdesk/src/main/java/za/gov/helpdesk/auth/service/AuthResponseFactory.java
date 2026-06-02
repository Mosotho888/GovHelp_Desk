package za.gov.helpdesk.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.users.converter.UserMapper;
import za.gov.helpdesk.users.model.User;

@Component
@RequiredArgsConstructor
public class AuthResponseFactory {

    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    public AuthResponse build(User user, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiryMs / 1000)
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
