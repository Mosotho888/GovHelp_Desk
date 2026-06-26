package za.gov.helpdesk.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.users.mapper.UserMapper;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Factory component responsible for compiling and assembling uniform authentication responses.
 * Encapsulates token lifecycle generation and maps internal domain user entities safely into secure
 * data transfer representations.
 */
@Component
@RequiredArgsConstructor
public class AuthResponseFactory {

    private static final long MILLISECONDS_PER_SECOND = 1000L;

    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    /**
     * Constructs a populated {@link AuthResponse} data transfer instance for a user session.
     * Generates a new short-lived access token, computes its relative longevity duration, and
     * structures matching user profile details.
     *
     * @param user the authenticated domain {@link User} entity context
     * @param refreshToken the unexpired matching tracking session refresh token string
     * @return a unified token mapping wrapper object
     */
    public AuthResponse build(final User user, final String refreshToken) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiryMs / MILLISECONDS_PER_SECOND)
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
