package za.gov.helpdesk.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auditlog.model.AuditLog;
import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;
import za.gov.helpdesk.auth.model.PasswordResetToken;
import za.gov.helpdesk.auth.repository.PasswordResetTokenRepository;
import za.gov.helpdesk.auth.service.PasswordResetService;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.notification.messaging.PasswordResetEmailNotificationPublisher;
import za.gov.helpdesk.notification.service.ticket.TicketEmailService;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final TicketEmailService ticketEmailService;
    private final AuditEventPublisher auditPublisher;
    private final PasswordResetEmailNotificationPublisher emailPublisher;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_EXPIRY_MIN = 15;

    @Override
    @Transactional
    public void requestReset(PasswordResetRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            tokenRepository.invalidateAllByEmail(user.getEmail());

            String rawOtp  = generateOtp();
            String hashedOtp = passwordEncoder.encode(rawOtp);

            tokenRepository.save(PasswordResetToken.builder()
                    .email(user.getEmail())
                    .otpHash(hashedOtp)
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MIN))
                    .build());

            emailPublisher.publish(
                    user.getEmail(),
                    user.getName(),
                    rawOtp,
                    OTP_EXPIRY_MIN
            );

            log.info("Password reset OTP issued for: {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {

        PasswordResetToken token = tokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired OTP"));

        token.setAttempts(token.getAttempts() + 1);
        tokenRepository.save(token);

        if (!token.isValid()) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        if (!passwordEncoder.matches(request.getOtp(), token.getOtpHash())) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired OTP"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        refreshTokenService.revokeAll(user);

        auditPublisher.publishAuthAudit(
                AuditLog.AuditAction.PASSWORD_RESET,
                user.getId(), user.getName(), user.getRole().name(),
                "Password reset via OTP"
        );

        log.info("Password reset completed for: {}", user.getEmail());
    }

    @Override
    public void purgeExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now().minusHours(1));

        log.info("Expired password reset tokens purged");
    }

    private String generateOtp() {

        int otp = 100_000 + RANDOM.nextInt(900_000);

        return String.valueOf(otp);
    }
}
