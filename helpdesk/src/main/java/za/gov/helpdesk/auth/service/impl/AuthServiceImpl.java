package za.gov.helpdesk.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.auth.dto.request.LoginRequest;
import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.converter.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.security.CustomUserDetails;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String TOKEN_REFRESH = "refresh";
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final AuditEventPublisher auditPublisher;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest loginRequest) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Reset on successful login
            user.setLoginAttempts(0);
            userRepository.save(user);

            String refreshToken = jwtService.generateRefreshToken(user);
            refreshTokenService.store(refreshToken, user);

            auditPublisher.publishAuthAudit(
                    AuditLog.AuditAction.LOGIN_SUCCESS,
                    user.getId(), user.getName(), user.getRole().name(),
                    "Login successful"
            );

            return buildAuthResponse(user, refreshToken);

        } catch (BadCredentialsException ex) {
            handleFailedLogin(loginRequest.getEmail());

            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest refreshToken) {

        if (jwtService.isRefreshToken(refreshToken.getRefreshToken()) || jwtService.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        RefreshToken stored = refreshTokenService.validate(refreshToken.getRefreshToken());
        User user = stored.getUser();

        String newRefreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(newRefreshToken, user);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.TOKEN_REFRESHED,
                user.getId(), user.getName(), user.getRole().name(),
                "Access token refreshed"
        );

        return buildAuthResponse(user, newRefreshToken);
    }

    private void handleFailedLogin(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    int attempts = user.getLoginAttempts() + 1;

                    user.setLoginAttempts(attempts);

                    if (attempts >= maxLoginAttempts) {
                        user.setActive(false);

                        auditPublisher.publishAuthAudit(
                                AuditLog.AuditAction.ACCOUNT_LOCKED,
                                user.getId(), user.getName(), user.getRole().name(),
                                attempts + " consecutive failed login attempts"
                        );
                    }

                    userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken, User actor) {
        refreshTokenService.revokeAll(actor);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.FORCED_LOGOUT,
                actor.getId(), actor.getName(), actor.getRole().name(),
                "User logged out");
    }

    private AuthResponse buildAuthResponse(User user,  String refreshToken) {

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiryMs / 1000)
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
