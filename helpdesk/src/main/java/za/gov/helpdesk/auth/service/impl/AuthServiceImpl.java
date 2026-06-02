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
import za.gov.helpdesk.auth.policy.LoginLockoutService;
import za.gov.helpdesk.auth.service.AuthResponseFactory;
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

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditEventPublisher auditPublisher;
    private final RefreshTokenService refreshTokenService;
    private final LoginLockoutService lockoutService;
    private final AuthResponseFactory authResponseFactory;

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

            return authResponseFactory.build(user, refreshToken);

        } catch (BadCredentialsException ex) {

            lockoutService.recordFailedAttempt(loginRequest.getEmail());
            throw ex;
        }
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest refreshToken) {

        String rawRefreshToken = refreshToken.getRefreshToken();

        if (!jwtService.isRefreshToken(rawRefreshToken) || jwtService.isTokenExpired(rawRefreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        RefreshToken stored = refreshTokenService.validate(rawRefreshToken);
        User user = stored.getUser();

        String newRefreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(newRefreshToken, user);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.TOKEN_REFRESHED,
                user.getId(), user.getName(), user.getRole().name(),
                "Access token refreshed"
        );

        return authResponseFactory.build(user, newRefreshToken);
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
}
