package za.gov.helpdesk.unit.services.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import za.gov.helpdesk.auth.model.RefreshToken;
import za.gov.helpdesk.auth.repository.RefreshTokenRepository;
import za.gov.helpdesk.auth.service.impl.RefreshTokenServiceImpl;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImpl unit tests")
class RefreshTokenServiceImplTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Captor private ArgumentCaptor<RefreshToken> tokenCaptor;

    private RefreshTokenServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(refreshTokenRepository);
        ReflectionTestUtils.setField(service, "refreshTokenExpiryMs", 604_800_000);

        user =
                User.builder()
                        .id(1L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(Role.USER)
                        .active(true)
                        .build();
    }

    @Test
    @DisplayName("store() revokes existing tokens before saving the new one")
    void store_validToken_revokesThenSaves() {
        service.store("raw-token-value", user);

        then(refreshTokenRepository).should(times(1)).revokeAllByUser(user);
        then(refreshTokenRepository).should(times(1)).save(tokenCaptor.capture());

        final RefreshToken saved = tokenCaptor.getValue();
        assertThat(saved.getToken()).isEqualTo("raw-token-value");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("validate() returns the token when it is unrevoked and unexpired")
    void validate_activeToken_returnsToken() {
        final RefreshToken token =
                RefreshToken.builder()
                        .token("valid-token")
                        .user(user)
                        .revoked(false)
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build();
        given(refreshTokenRepository.findByToken("valid-token")).willReturn(Optional.of(token));

        final RefreshToken result = service.validate("valid-token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("validate() throws for an unknown token")
    void validate_unknownToken_throwsBadCredentials() {
        given(refreshTokenRepository.findByToken("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("missing"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("validate() throws for a revoked token")
    void validate_revokedToken_throwsBadCredentials() {
        final RefreshToken token =
                RefreshToken.builder()
                        .token("revoked-token")
                        .user(user)
                        .revoked(true)
                        .expiresAt(LocalDateTime.now().plusDays(1))
                        .build();
        given(refreshTokenRepository.findByToken("revoked-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validate("revoked-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("validate() throws for an expired token")
    void validate_expiredToken_throwsBadCredentials() {
        final RefreshToken token =
                RefreshToken.builder()
                        .token("expired-token")
                        .user(user)
                        .revoked(false)
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .build();
        given(refreshTokenRepository.findByToken("expired-token")).willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validate("expired-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("revokeAll() delegates straight to the repository for the given user")
    void revokeAll_anyUser_delegatesToRepository() {
        service.revokeAll(user);

        then(refreshTokenRepository).should(times(1)).revokeAllByUser(user);
    }

    @Test
    @DisplayName("purgeExpiredTokens() deletes tokens expired before the cutoff")
    void purgeExpiredTokens_anyState_deletesBeforeCutoff() {
        service.purgeExpiredTokens();

        then(refreshTokenRepository).should(times(1)).deleteExpiredBefore(any(LocalDateTime.class));
    }
}
