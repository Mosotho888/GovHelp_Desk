package za.gov.helpdesk.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.auth.dto.request.LoginRequest;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.dto.response.UserResponse;
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
    private final JwtService jwtService;
    private final AuditEventPublisher auditPublisher;

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

            auditPublisher.publishAuthAudit(
                    AuditLog.AuditAction.LOGIN_SUCCESS,
                    user.getId(), user.getName(), user.getRole().name(),
                    "Login successful"
            );

            return buildAuthResponse(user);

        } catch (BadCredentialsException ex) {
            handleFailedLogin(loginRequest.getEmail());

            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest refreshToken) {
        String email = jwtService.extractEmail(refreshToken.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!TOKEN_REFRESH.equals(jwtService.extractTokenType(refreshToken.getRefreshToken()))
                || jwtService.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.TOKEN_REFRESHED,
                user.getId(), user.getName(), user.getRole().name(),
                "Access token refreshed"
        );

        return buildAuthResponse(user);
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

    private AuthResponse buildAuthResponse(User user) {

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .expiresIn(accessTokenExpiryMs / 1000)
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .timezone(user.getTimezone())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
