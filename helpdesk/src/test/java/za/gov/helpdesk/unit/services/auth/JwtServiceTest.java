package za.gov.helpdesk.unit.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import za.gov.helpdesk.auth.jwt.JwtService;
import za.gov.helpdesk.exception.InvalidTokenException;
import za.gov.helpdesk.users.model.Role;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.security.CustomUserDetails;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("JwtService unit tests")
class JwtServiceTest {

    // Base64-encoded 256+ bit secret, matching what app.jwt.secret expects (HMAC-SHA key material).
    private static final String SECRET = "5JzoMbk6E5qIqHSuBTgeQCARtUsxAkBiHwdjXOSW8kWdXzYmP3X51C0";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryMs", 604_800_000L);

        user =
                User.builder()
                        .id(1L)
                        .name("Jane Agent")
                        .email("jane@gov.za")
                        .role(Role.AGENT)
                        .active(true)
                        .build();
    }

    @Test
    @DisplayName("generateAccessToken() produces a token whose subject is the user's email")
    void generateAccessToken_validUser_subjectIsEmail() {
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo("jane@gov.za");
    }

    @Test
    @DisplayName("generateAccessToken() tags the token type as \"access\"")
    void generateAccessToken_validUser_typeIsAccess() {
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("generateAccessToken() embeds the user's role as a claim")
    void generateAccessToken_validUser_embedsRole() {
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractClaims(token).get("role")).isEqualTo("AGENT");
    }

    @Test
    @DisplayName("generateRefreshToken() tags the token type as \"refresh\"")
    void generateRefreshToken_validUser_typeIsRefresh() {
        final String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns true for a fresh token matching the principal")
    void isTokenValid_matchingPrincipal_returnsTrue() {
        final String token = jwtService.generateAccessToken(user);
        final CustomUserDetails principal = new CustomUserDetails(user);

        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false when the token subject does not match the principal")
    void isTokenValid_mismatchedPrincipal_returnsFalse() {
        final String token = jwtService.generateAccessToken(user);
        final User otherUser =
                User.builder()
                        .id(2L)
                        .name("Someone Else")
                        .email("other@gov.za")
                        .role(Role.USER)
                        .active(true)
                        .build();

        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(otherUser))).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired() returns false for a freshly issued token")
    void isTokenExpired_freshToken_returnsFalse() {
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired() returns true once the expiry window has already elapsed")
    void isTokenExpired_alreadyExpiredWindow_returnsTrue() {
        // Set expiry to a negative window so the generated token is expired the instant it's
        // minted.
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", -1_000L);
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false once the token has expired")
    void isTokenValid_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", -1_000L);
        final String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(user))).isFalse();
    }

    @Test
    @DisplayName("extractClaims() rejects a structurally malformed token")
    void extractClaims_malformedToken_throwsInvalidTokenException() {
        assertThatThrownBy(() -> jwtService.extractClaims("not-a-real-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("extractClaims() rejects a token tampered with after signing")
    void extractClaims_tamperedSignature_throwsInvalidTokenException() {
        final String token = jwtService.generateAccessToken(user);
        // Flip the last character of the signature segment to corrupt it.
        final String tampered =
                token.substring(0, token.length() - 1)
                        + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwtService.extractClaims(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("extractClaims() rejects a token signed with a different secret")
    void extractClaims_differentSigningKey_throwsInvalidTokenException() {
        final String token = jwtService.generateAccessToken(user);

        final JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(
                otherService, "secret", "9dNwrq5vX2QeYVh6b7ktEaLsWZmupTzHo1RcnBiKfP4jGxCyOl8AaSU3");
        ReflectionTestUtils.setField(otherService, "accessTokenExpiryMs", 3_600_000L);

        assertThatThrownBy(() -> otherService.extractClaims(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("access and refresh tokens generated for the same user are distinct")
    void generateTokens_sameUser_accessAndRefreshDiffer() {
        final String accessToken = jwtService.generateAccessToken(user);
        final String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }
}
