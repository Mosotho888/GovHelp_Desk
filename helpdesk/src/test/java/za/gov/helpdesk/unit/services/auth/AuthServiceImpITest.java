package za.gov.helpdesk.unit.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
import za.gov.helpdesk.auth.service.RefreshTokenService;
import za.gov.helpdesk.auth.service.impl.AuthServiceImpl;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;
import za.gov.helpdesk.users.security.CustomUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
public class AuthServiceImpITest {

    @Mock private AuthenticationManager authManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private AuditEventPublisher auditPublisher;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginLockoutService lockoutService;
    @Mock private AuthResponseFactory authResponseFactory;
    @Mock private AuthMetrics authMetrics;

    @InjectMocks private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser =
                User.builder()
                        .id(1L)
                        .name("Test Agent")
                        .email("agent@gov.za")
                        .passwordHash(
                                "$2a$12$xUDVJeAkGqB7hUk0je5oZe2s569JMXdQXU.bIyViyZrF2SBxWtuei")
                        .role(User.Role.AGENT)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build();
    }

    @Test
    @DisplayName("login() returns AuthResponse and resets login counter on valid credentials")
    void login_validCredentials_returnsTokensAndResetsCounter() {
        final LoginRequest request = loginRequest("agent@gov.za", "Password@123");
        final CustomUserDetails principal = new CustomUserDetails(testUser);
        final AuthResponse expected = authResponse();

        given(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));

        given(jwtService.generateRefreshToken(testUser)).willReturn("refresh.token.here");
        given(authResponseFactory.build(eq(testUser), any())).willReturn(expected);

        final AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();

        then(lockoutService).should(times(1)).resetFailedAttempts(testUser);

        then(authMetrics).should(times(1)).incrementLoginSuccess();

        then(auditPublisher)
                .should(times(1))
                .publishAuthAudit(
                        eq(AuditLog.AuditAction.LOGIN_SUCCESS),
                        eq(testUser.getId()),
                        eq(testUser.getName()),
                        eq(testUser.getRole().name()),
                        any());
    }

    @Test
    @DisplayName("login() delegates failed attempt to LoginLockoutService on bad credentials")
    void login_badCredentials_delegatesToLockoutService() {
        final LoginRequest request = loginRequest("agent@gov.za", "WrongPassword");

        given(authManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        then(authMetrics).should(times(1)).incrementLoginFailure();

        then(lockoutService).should(times(1)).recordFailedAttempt("agent@gov.za");
    }

    @Test
    @DisplayName("login() does not call lockout service on successful authentication")
    void login_successfulLogin_doesNotCallLockoutService() {
        final CustomUserDetails principal = new CustomUserDetails(testUser);

        given(authManager.authenticate(any()))
                .willReturn(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));

        given(jwtService.generateRefreshToken(testUser)).willReturn("refresh.token");
        given(authResponseFactory.build(eq(testUser), any())).willReturn(authResponse());

        final LoginRequest request = loginRequest("agent@gov.za", "Password@123");
        authService.login(request);

        then(lockoutService).should(times(1)).resetFailedAttempts(testUser);
    }

    @Test
    @DisplayName("login() stores refresh token via RefreshTokenService")
    void login_successful_storesRefreshToken() {
        final CustomUserDetails principal = new CustomUserDetails(testUser);

        given(authManager.authenticate(any()))
                .willReturn(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));
        given(jwtService.generateRefreshToken(testUser)).willReturn("refresh.token.here");
        given(authResponseFactory.build(eq(testUser), any())).willReturn(authResponse());

        final LoginRequest request = loginRequest("agent@gov.za", "Password@123");
        authService.login(request);

        then(authMetrics).should(times(1)).incrementLoginSuccess();

        then(refreshTokenService).should(times(1)).store("refresh.token.here", testUser);
    }

    @Test
    @DisplayName("refresh() returns new tokens for a valid, unexpired refresh token")
    void refresh_validToken_returnsNewTokens() {
        final RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(testUser);
        final RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("valid.refresh.token");

        given(jwtService.isRefreshToken("valid.refresh.token")).willReturn(true);
        given(jwtService.isTokenExpired("valid.refresh.token")).willReturn(false);
        given(refreshTokenService.validate("valid.refresh.token")).willReturn(storedToken);
        given(jwtService.generateRefreshToken(testUser)).willReturn("new.refresh.token");
        given(authResponseFactory.build(eq(testUser), any())).willReturn(authResponse());

        final AuthResponse response = authService.refresh(req);

        then(authMetrics).should(times(1)).incrementTokenRefreshed();

        assertThat(response).isNotNull();
        then(refreshTokenService).should(times(1)).store("new.refresh.token", testUser);
        then(auditPublisher)
                .should(times(1))
                .publishAuthAudit(
                        eq(AuditLog.AuditAction.TOKEN_REFRESHED),
                        eq(testUser.getId()),
                        eq(testUser.getName()),
                        eq(testUser.getRole().name()),
                        any());
    }

    @Test
    @DisplayName("refresh() throws BadCredentialsException for an expired token")
    void refresh_expiredToken_throws() {
        final RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("expired.token");

        given(jwtService.isRefreshToken("expired.token")).willReturn(true);
        given(jwtService.isTokenExpired("expired.token")).willReturn(true);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("refresh() throws BadCredentialsException when token is not a refresh token")
    void refresh_notRefreshToken_throws() {
        final RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("access.token");

        given(jwtService.isRefreshToken("access.token")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("logout() revokes all tokens and publishes FORCED_LOGOUT audit")
    void logout_valid_revokesAllAndPublishesAudit() {
        authService.logout("any.token", testUser);

        then(authMetrics).should(times(1)).incrementLogout();
        then(refreshTokenService).should(times(1)).revokeAll(testUser);
        then(auditPublisher)
                .should(times(1))
                .publishAuthAudit(
                        eq(AuditLog.AuditAction.FORCED_LOGOUT),
                        eq(testUser.getId()),
                        eq(testUser.getName()),
                        eq(testUser.getRole().name()),
                        any());
    }

    private LoginRequest loginRequest(final String email, final String password) {
        final LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("access.token.here")
                .refreshToken("refresh.token.here")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(
                        UserResponse.builder()
                                .id(testUser.getId())
                                .name(testUser.getName())
                                .email(testUser.getEmail())
                                .role(testUser.getRole())
                                .active(testUser.getActive())
                                .build())
                .build();
    }
}
