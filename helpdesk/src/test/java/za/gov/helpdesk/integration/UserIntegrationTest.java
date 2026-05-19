package za.gov.helpdesk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User management integration tests")
public class UserIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("System Admin").email("admin@gov.za")
                .passwordHash(passwordEncoder.encode("AdminPass1!"))
                .role(User.Role.ADMIN).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());

        userRepository.save(User.builder()
                .name("John Public").email("john@citizen.za")
                .passwordHash(passwordEncoder.encode("UserPass1!"))
                .role(User.Role.USER).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());

        adminToken = login("admin@gov.za",      "AdminPass1!");
        userToken  = login("john@citizen.za",   "UserPass1!");
    }

    @Test
    @DisplayName("POST /users creates user (Admin only)")
    void createUser_adminToken_returns201() throws Exception {
        mvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name",     "New Citizen",
                                "email",    "newcitizen@gov.za",
                                "password", "Citizen@1234",
                                "role",     "USER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newcitizen@gov.za"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /users returns 403 for non-admin token")
    void createUser_userToken_returns403() throws Exception {
        mvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Hacker", "email", "hacker@evil.com", "password", "Hack@1234"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /users returns 409 for duplicate email")
    void createUser_duplicateEmail_returns409() throws Exception {
        mvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Duplicate", "email", "john@citizen.za", "password", "Pass@1234"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("POST /users returns 400 for weak password")
    void createUser_weakPassword_returns400() throws Exception {
        mvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "Test", "email", "test@gov.za", "password", "weak"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /users/{id} deactivates user (Admin only)")
    void deactivateUser_adminToken_returns204() throws Exception {
        // Create a user to deactivate
        String body = mvc.perform(post("/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", "To Deactivate", "email", "deactivate@gov.za", "password", "Temp@1234"
                        ))))
                .andReturn().getResponse().getContentAsString();

        long userId = mapper.readTree(body).get("id").asLong();

        mvc.perform(delete("/v1/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify they can no longer login
        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "deactivate@gov.za", "password", "Temp@1234"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users returns 403 for non-admin token")
    void listUsers_userToken_returns403() throws Exception {
        mvc.perform(get("/v1/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ── Helper ────────────────────────────────────────────────
    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
