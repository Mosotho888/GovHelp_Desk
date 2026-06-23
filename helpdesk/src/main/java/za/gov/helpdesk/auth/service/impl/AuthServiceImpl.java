package za.gov.helpdesk.auth.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.dto.request.LoginRequest;
import za.gov.helpdesk.auth.dto.request.RefreshTokenRequest;
import za.gov.helpdesk.auth.dto.response.AuthResponse;
import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.auth.metrics.AuthMetrics;
import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.auth.policy.LoginLockoutService;
import za.gov.helpdesk.auth.service.AuthResponseFactory;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditEventPublisher auditPublisher;
    private final RefreshTokenService refreshTokenService;
    private final LoginLockoutService lockoutService;
    private final AuthResponseFactory authResponseFactory;
    private final AuthMetrics authMetrics;

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(final LoginRequest loginRequest) {

        final String email =
                loginRequest.getEmail() != null ? loginRequest.getEmail().toLowerCase().trim() : "";

        try {
            final Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email, loginRequest.getPassword()));

            final CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            final User user = userDetails.getUser();

            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new DisabledException("Account is inactive");
            }

            lockoutService.resetFailedAttempts(user);

            final String refreshToken = jwtService.generateRefreshToken(user);
            refreshTokenService.store(refreshToken, user);

            auditPublisher.publishAuthAudit(
                    AuditLog.AuditAction.LOGIN_SUCCESS,
                    user.getId(),
                    user.getName(),
                    user.getRole().name(),
                    "Login successful");

            authMetrics.incrementLoginSuccess();

            return authResponseFactory.build(user, refreshToken);

        } catch (final BadCredentialsException ex) {

            lockoutService.recordFailedAttempt(email);
            authMetrics.incrementLoginFailure();
            throw ex;
        }
    }

    @Override
    @Transactional
    public AuthResponse refresh(final RefreshTokenRequest refreshToken) {

        final String rawRefreshToken = refreshToken.getRefreshToken();

        if (!jwtService.isRefreshToken(rawRefreshToken)
                || jwtService.isTokenExpired(rawRefreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        final RefreshToken stored = refreshTokenService.validate(rawRefreshToken);
        final User user = stored.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("Session renewal rejected: Account is inactive");
        }

        final String newRefreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(newRefreshToken, user);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.TOKEN_REFRESHED,
                user.getId(),
                user.getName(),
                user.getRole().name(),
                "Access token refreshed");

        authMetrics.incrementTokenRefreshed();

        return authResponseFactory.build(user, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(final String rawRefreshToken, final User actor) {
        refreshTokenService.revokeAll(actor);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.FORCED_LOGOUT,
                actor.getId(),
                actor.getName(),
                actor.getRole().name(),
                "User logged out");

        authMetrics.incrementLogout();
    }
}
