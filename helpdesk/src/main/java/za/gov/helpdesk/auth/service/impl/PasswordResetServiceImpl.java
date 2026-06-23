package za.gov.helpdesk.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;
import za.gov.helpdesk.auth.metrics.AuthMetrics;
import za.gov.helpdesk.auth.model.PasswordResetToken;
import za.gov.helpdesk.auth.repository.PasswordResetTokenRepository;
import za.gov.helpdesk.auth.service.OtpGeneratorService;
import za.gov.helpdesk.auth.service.PasswordResetService;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.notification.messaging.PasswordResetEmailNotificationPublisher;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final long OTP_EXPIRY_MIN = 15;
    private static final int INCREMENT_BY_ONE = 1;
    private static final int HOURS = 1;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final OtpGeneratorService otpGeneratorService;
    private final AuditEventPublisher auditPublisher;
    private final PasswordResetEmailNotificationPublisher emailPublisher;
    private final AuthMetrics authMetrics;

    @Override
    @Transactional
    public void requestReset(final PasswordResetRequest request) {

        userRepository
                .findByEmail(request.getEmail())
                .ifPresent(
                        user -> {
                            tokenRepository.invalidateAllByEmail(user.getEmail());

                            final String rawOtp = otpGeneratorService.generate();
                            final String hashedOtp = passwordEncoder.encode(rawOtp);

                            tokenRepository.save(
                                    PasswordResetToken.builder()
                                            .email(user.getEmail())
                                            .otpHash(hashedOtp)
                                            .expiresAt(
                                                    LocalDateTime.now().plusMinutes(OTP_EXPIRY_MIN))
                                            .build());

                            emailPublisher.publish(
                                    user.getEmail(), user.getName(), rawOtp, OTP_EXPIRY_MIN);

                            authMetrics.incrementPasswordResetRequested();

                            log.info("Password reset OTP issued for: {}", user.getEmail());
                        });
    }

    @Override
    @Transactional
    public void confirmReset(final PasswordResetConfirmRequest request) {

        final PasswordResetToken token =
                tokenRepository
                        .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(request.getEmail())
                        .orElseThrow(() -> new BadCredentialsException("Invalid or expired OTP"));

        token.setAttempts(token.getAttempts() + INCREMENT_BY_ONE);
        tokenRepository.save(token);

        if (!token.isValid()) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        if (!passwordEncoder.matches(request.getOtp(), token.getOtpHash())) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        final User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() -> new BadCredentialsException("Invalid or expired OTP"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        refreshTokenService.revokeAll(user);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.PASSWORD_RESET,
                user.getId(),
                user.getName(),
                user.getRole().name(),
                "Password reset via OTP");

        authMetrics.incrementPasswordResetConfirmed();

        log.info("Password reset completed for: {}", user.getEmail());
    }

    @Override
    public void purgeExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now().minusHours(HOURS));

        log.info("Expired password reset tokens purged");
    }
}
