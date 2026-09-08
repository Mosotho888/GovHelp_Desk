package za.gov.helpdesk.unit.services.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import za.gov.helpdesk.auditlog.messaging.AuditEventPublisher;
import za.gov.helpdesk.auth.dto.request.PasswordResetConfirmRequest;
import za.gov.helpdesk.auth.dto.request.PasswordResetRequest;
import za.gov.helpdesk.auth.metrics.AuthMetrics;
import za.gov.helpdesk.auth.model.PasswordResetToken;
import za.gov.helpdesk.auth.repository.PasswordResetTokenRepository;
import za.gov.helpdesk.auth.service.OtpGeneratorService;
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.auth.service.impl.PasswordResetServiceImpl;
import za.gov.helpdesk.notification.messaging.PasswordResetEmailNotificationPublisher;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl unit tests")
class PasswordResetServiceImplTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OtpGeneratorService otpGeneratorService;
    @Mock private AuditEventPublisher auditPublisher;
    @Mock private PasswordResetEmailNotificationPublisher emailPublisher;
    @Mock private AuthMetrics authMetrics;

    private PasswordResetServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        service =
                new PasswordResetServiceImpl(
                        tokenRepository,
                        userRepository,
                        passwordEncoder,
                        refreshTokenService,
                        otpGeneratorService,
                        auditPublisher,
                        emailPublisher,
                        authMetrics);

        user =
                User.builder()
                        .id(1L)
                        .name("John Public")
                        .email("john@citizen.za")
                        .role(Role.USER)
                        .active(true)
                        .build();
    }

    // ---- requestReset ----

    @Test
    @DisplayName("requestReset() invalidates old tokens, issues a new OTP, and emails it")
    void requestReset_knownEmail_issuesOtpAndEmails() {
        final PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("john@citizen.za");

        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(user));
        given(otpGeneratorService.generate()).willReturn("123456");
        given(passwordEncoder.encode("123456")).willReturn("hashed-otp");

        service.requestReset(request);

        then(tokenRepository).should(times(1)).invalidateAllByEmail("john@citizen.za");
        then(tokenRepository).should(times(1)).save(any(PasswordResetToken.class));
        then(emailPublisher)
                .should(times(1))
                .publish(eq("john@citizen.za"), eq("John Public"), eq("123456"), eq(15L));
        then(authMetrics).should(times(1)).incrementPasswordResetRequested();
    }

    @Test
    @DisplayName("requestReset() is a silent no-op for an unknown email (no user enumeration)")
    void requestReset_unknownEmail_doesNothingObservable() {
        final PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("ghost@nowhere.za");
        given(userRepository.findByEmail("ghost@nowhere.za")).willReturn(Optional.empty());

        service.requestReset(request);

        then(tokenRepository).should(never()).save(any());
        then(emailPublisher)
                .should(never())
                .publish(anyString(), anyString(), anyString(), anyLong());
        then(authMetrics).should(never()).incrementPasswordResetRequested();
    }

    // ---- confirmReset ----

    @Test
    @DisplayName("confirmReset() updates the password and revokes sessions on a valid OTP")
    void confirmReset_validOtp_updatesPasswordAndRevokesTokens() {
        final PasswordResetToken token =
                PasswordResetToken.builder()
                        .email("john@citizen.za")
                        .otpHash("hashed-otp")
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .used(false)
                        .attempts(0)
                        .build();
        final PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setEmail("john@citizen.za");
        request.setOtp("123456");
        request.setNewPassword("newSecurePass1");

        given(tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("john@citizen.za"))
                .willReturn(Optional.of(token));
        given(passwordEncoder.matches("123456", "hashed-otp")).willReturn(true);
        given(userRepository.findByEmail("john@citizen.za")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newSecurePass1")).willReturn("new-hash");

        service.confirmReset(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
        then(refreshTokenService).should(times(1)).revokeAll(user);
        then(authMetrics).should(times(1)).incrementPasswordResetConfirmed();
    }

    @Test
    @DisplayName("confirmReset() throws when no unused OTP token exists for the email")
    void confirmReset_noToken_throwsBadCredentials() {
        final PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setEmail("john@citizen.za");
        request.setOtp("123456");
        request.setNewPassword("newSecurePass1");

        given(tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("john@citizen.za"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmReset(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    @DisplayName("confirmReset() throws when the token has expired")
    void confirmReset_expiredToken_throwsBadCredentials() {
        final PasswordResetToken token =
                PasswordResetToken.builder()
                        .email("john@citizen.za")
                        .otpHash("hashed-otp")
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .used(false)
                        .attempts(0)
                        .build();
        final PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setEmail("john@citizen.za");
        request.setOtp("123456");
        request.setNewPassword("newSecurePass1");

        given(tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("john@citizen.za"))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmReset(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    @DisplayName("confirmReset() throws once the token has exceeded its maximum attempt count")
    void confirmReset_tooManyAttempts_throwsBadCredentials() {
        final PasswordResetToken token =
                PasswordResetToken.builder()
                        .email("john@citizen.za")
                        .otpHash("hashed-otp")
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .used(false)
                        .attempts(PasswordResetToken.MAX_ATTEMPTS - 1)
                        .build();
        final PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setEmail("john@citizen.za");
        request.setOtp("wrong");
        request.setNewPassword("newSecurePass1");

        given(tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("john@citizen.za"))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.confirmReset(request))
                .isInstanceOf(BadCredentialsException.class);
        // attempts is incremented before the validity check runs, so it should now be at the max
        assertThat(token.getAttempts()).isEqualTo(PasswordResetToken.MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("confirmReset() throws when the supplied OTP does not match the stored hash")
    void confirmReset_wrongOtp_throwsBadCredentials() {
        final PasswordResetToken token =
                PasswordResetToken.builder()
                        .email("john@citizen.za")
                        .otpHash("hashed-otp")
                        .expiresAt(LocalDateTime.now().plusMinutes(10))
                        .used(false)
                        .attempts(0)
                        .build();
        final PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setEmail("john@citizen.za");
        request.setOtp("999999");
        request.setNewPassword("newSecurePass1");

        given(tokenRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc("john@citizen.za"))
                .willReturn(Optional.of(token));
        given(passwordEncoder.matches("999999", "hashed-otp")).willReturn(false);

        assertThatThrownBy(() -> service.confirmReset(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid or expired OTP");

        then(userRepository).should(never()).save(any());
    }

    // ---- purgeExpiredTokens ----

    @Test
    @DisplayName("purgeExpiredTokens() delegates to the repository with a one-hour cutoff")
    void purgeExpiredTokens_anyState_delegatesToRepository() {
        service.purgeExpiredTokens();

        then(tokenRepository).should(times(1)).deleteExpiredBefore(any(LocalDateTime.class));
    }
}
