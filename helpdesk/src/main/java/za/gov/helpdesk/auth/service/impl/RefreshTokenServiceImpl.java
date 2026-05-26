package za.gov.helpdesk.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.auth.repository.RefreshTokenRepository;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.users.model.User;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private int refreshTokenExpiryMs;

    @Override
    @Transactional
    public void store(String rawToken, User user) {

        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken token = RefreshToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpiryMs / 1000))
                .build();

        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String rawToken) {

        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
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
    public void revokeAll(User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("All refresh tokens revoked for user: {}", user.getEmail());
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Expired refresh tokens purged");
    }
}
