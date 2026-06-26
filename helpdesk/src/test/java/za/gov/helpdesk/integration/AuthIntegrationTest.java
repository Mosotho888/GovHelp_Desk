package za.gov.helpdesk.integration;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.auth.repository.RefreshTokenRepository;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth integration tests")
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(
                User.builder()
                        .name("Test Agent")
                        .email("agent@gov.za")
                        .passwordHash(passwordEncoder.encode("ValidPass1!"))
                        .role(User.Role.AGENT)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());
    }

    @Test
    @DisplayName("POST /auth/login returns 200 and tokens for valid credentials")
    void login_validCredentials_returns200WithTokens() throws Exception {
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "agent@gov.za",
                                                        "password",
                                                        "ValidPass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("agent@gov.za"))
                .andExpect(jsonPath("$.user.role").value("AGENT"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "agent@gov.za",
                                                        "password",
                                                        "WrongPassword1!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 for non-existent email")
    void login_unknownEmail_returns401() throws Exception {
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "nobody@gov.za",
                                                        "password",
                                                        "ValidPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login returns 400 for missing email")
    void login_missingEmail_returns400() throws Exception {
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("password", "ValidPass1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /auth/login returns 400 for invalid email format")
    void login_invalidEmailFormat_returns400() throws Exception {
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "not-an-email",
                                                        "password",
                                                        "ValidPass1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    @Test
    @DisplayName("POST /auth/login returns 403 for inactive account")
    void login_inactiveAccount_returns403() throws Exception {
        userRepository.save(
                User.builder()
                        .name("Inactive User")
                        .email("inactive@gov.za")
                        .passwordHash(passwordEncoder.encode("ValidPass1!"))
                        .role(User.Role.USER)
                        .active(false)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());

        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "inactive@gov.za",
                                                        "password",
                                                        "ValidPass1!"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/login locks account after 5 failed attempts")
    void login_fiveFailedAttempts_locksAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(
                    post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    mapper.writeValueAsString(
                                            Map.of(
                                                    "email",
                                                    "agent@gov.za",
                                                    "password",
                                                    "WrongPass1!"))));
        }

        // 6th attempt with correct password is still locked
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "agent@gov.za",
                                                        "password",
                                                        "ValidPass1!"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/refresh returns new access token from valid refresh token")
    void refresh_validRefreshToken_returnsNewAccessToken() throws Exception {
        final String body =
                mvc.perform(
                                post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                "agent@gov.za",
                                                                "password",
                                                                "ValidPass1!"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final String refreshToken = mapper.readTree(body).get("refreshToken").asText();

        mvc.perform(
                        post("/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/refresh returns 401 for invalid refresh token")
    void refresh_invalidToken_returns401() throws Exception {
        mvc.perform(
                        post("/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("refreshToken", "bogus.token.value"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Malformed token"));
    }

    @Test
    @DisplayName("POST /auth/logout revokes refresh token — subsequent refresh returns 401")
    void logout_revokesRefreshToken() throws Exception {
        final String body =
                mvc.perform(
                                post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                "agent@gov.za",
                                                                "password",
                                                                "ValidPass1!"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        final String accessToken = mapper.readTree(body).get("accessToken").asText();
        final String refreshToken = mapper.readTree(body).get("refreshToken").asText();

        mvc.perform(
                        post("/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        // Revoked token must no longer work
        mvc.perform(
                        post("/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/password-reset/request always returns 200 (user enumeration safe)")
    void passwordResetRequest_alwaysReturns200() throws Exception {
        // Known email
        mvc.perform(
                        post("/v1/auth/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(Map.of("email", "agent@gov.za"))))
                .andExpect(status().isOk());

        // Unknown email - must also return 200 to prevent enumeration
        mvc.perform(
                        post("/v1/auth/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(Map.of("email", "ghost@gov.za"))))
                .andExpect(status().isOk());
    }
}
