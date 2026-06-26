package za.gov.helpdesk.auth.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.auth.repository.RefreshTokenRepository;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long MILLISECONDS_PER_SECOND = 1000L;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private int refreshTokenExpiryMs;

    @Override
    @Transactional
    public void store(final String rawToken, final User user) {

        refreshTokenRepository.revokeAllByUser(user);

        final RefreshToken token =
                RefreshToken.builder()
                        .token(rawToken)
                        .user(user)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusSeconds(
                                                refreshTokenExpiryMs / MILLISECONDS_PER_SECOND))
                        .build();

        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(final String rawToken) {

        final RefreshToken token =
                refreshTokenRepository
                        .findByToken(rawToken)
                        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (token.isExpired()) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeAll(final User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("All refresh tokens revoked for user: {}", user.getEmail());
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredTokens() {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Expired refresh tokens purged");
    }
}
