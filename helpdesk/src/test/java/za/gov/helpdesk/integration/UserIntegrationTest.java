package za.gov.helpdesk.integration;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User management integration tests")
public class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long johnId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(
                User.builder()
                        .name("System Admin")
                        .email("admin@gov.za")
                        .passwordHash(passwordEncoder.encode("AdminPass1!"))
                        .role(User.Role.ADMIN)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());

        final User john =
                userRepository.save(
                        User.builder()
                                .name("John Public")
                                .email("john@citizen.za")
                                .passwordHash(passwordEncoder.encode("UserPass1!"))
                                .role(User.Role.USER)
                                .active(true)
                                .loginAttempts(0)
                                .timezone("Africa/Johannesburg")
                                .build());
        johnId = john.getId();

        adminToken = login("admin@gov.za", "AdminPass1!");
        userToken = login("john@citizen.za", "UserPass1!");
    }

    @Test
    @DisplayName("POST /users creates user (Admin only) — returns 201")
    void createUser_adminToken_returns201() throws Exception {
        mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        "New Citizen",
                                                        "email",
                                                        "newcitizen@gov.za",
                                                        "password",
                                                        "Citizen@1234",
                                                        "role",
                                                        "USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newcitizen@gov.za"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /users returns 403 for non-admin token")
    void createUser_userToken_returns403() throws Exception {
        mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        "Hacker",
                                                        "email",
                                                        "hacker@evil.com",
                                                        "password",
                                                        "Hack@1234"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /users returns 409 for duplicate email")
    void createUser_duplicateEmail_returns409() throws Exception {
        mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        "Duplicate",
                                                        "email",
                                                        "john@citizen.za",
                                                        "password",
                                                        "Pass@1234"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("POST /users returns 400 for weak password")
    void createUser_weakPassword_returns400() throws Exception {
        mvc.perform(
                        post("/v1/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        "Test",
                                                        "email",
                                                        "test@gov.za",
                                                        "password",
                                                        "weak"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /users returns paginated list for ADMIN")
    void listUsers_adminToken_returns200() throws Exception {
        mvc.perform(get("/v1/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /users returns 403 for non-admin token")
    void listUsers_userToken_returns403() throws Exception {
        mvc.perform(get("/v1/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/me returns current user profile")
    void getMyProfile_authenticated_returnsOwnProfile() throws Exception {
        mvc.perform(get("/v1/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@citizen.za"));
    }

    @Test
    @DisplayName("GET /users/{id} allows admin to fetch any user")
    void getUserById_adminFetchesOtherUser_returns200() throws Exception {
        mvc.perform(get("/v1/users/" + johnId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@citizen.za"));
    }

    @Test
    @DisplayName("GET /users/{id} returns 403 when user tries to fetch another user's profile")
    void getUserById_idor_returns403() throws Exception {
        // john tries to fetch admin's profile — admin id is different from john's
        userRepository.save(
                User.builder()
                        .name("Another Citizen")
                        .email("another@citizen.za")
                        .passwordHash(passwordEncoder.encode("AnotherPass1!"))
                        .role(User.Role.USER)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());

        final String anotherToken = login("another@citizen.za", "AnotherPass1!");

        mvc.perform(get("/v1/users/" + johnId).header("Authorization", "Bearer " + anotherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /users/{id} allows user to update their own profile")
    void updateUser_ownProfile_returns200() throws Exception {
        mvc.perform(
                        put("/v1/users/" + johnId)
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "name",
                                                        "John Updated",
                                                        "timezone",
                                                        "Africa/Johannesburg"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));
    }

    @Test
    @DisplayName("DELETE /users/{id} deactivates user — deactivated user cannot login")
    void deactivateUser_adminToken_returns204AndPreventsLogin() throws Exception {
        final String body =
                mvc.perform(
                                post("/v1/users")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "name",
                                                                "To Deactivate",
                                                                "email",
                                                                "deactivate@gov.za",
                                                                "password",
                                                                "Temp@1234"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final long userId = mapper.readTree(body).get("id").asLong();

        mvc.perform(
                        delete("/v1/admin/users/" + userId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Deactivated users cannot log in
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "deactivate@gov.za",
                                                        "password",
                                                        "Temp@1234"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /users/{id}/reactivate re-enables account — user can login again")
    void reactivateUser_adminToken_allowsLoginAgain() throws Exception {
        // Create and deactivate
        final String body =
                mvc.perform(
                                post("/v1/users")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "name",
                                                                "Reactivatable",
                                                                "email",
                                                                "react@gov.za",
                                                                "password",
                                                                "React@1234"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final long userId = mapper.readTree(body).get("id").asLong();

        mvc.perform(
                        delete("/v1/admin/users/" + userId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Reactivate
        mvc.perform(
                        post("/v1/admin/users/" + userId + "/reactivate")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Should be able to login now
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "react@gov.za",
                                                        "password",
                                                        "React@1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /users/{id}/role changes user role (Admin only)")
    void changeRole_adminToken_returns200() throws Exception {
        mvc.perform(
                        patch("/v1/admin/users/" + johnId + "/role")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("role", "AGENT")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    @DisplayName("PATCH /users/{id}/role returns 403 for non-admin")
    void changeRole_nonAdmin_returns403() throws Exception {
        mvc.perform(
                        patch("/v1/admin/users/" + johnId + "/role")
                                .header("Authorization", "Bearer " + userToken)
                                .param("role", "ADMIN")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/me/password changes password with correct current password")
    void changeOwnPassword_validCurrentPassword_returns204() throws Exception {
        mvc.perform(
                        patch("/v1/users/me/password")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "currentPassword",
                                                        "UserPass1!",
                                                        "newPassword",
                                                        "NewUserPass1!"))))
                .andExpect(status().isNoContent());

        // Old password no longer works
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "john@citizen.za",
                                                        "password",
                                                        "UserPass1!"))))
                .andExpect(status().isUnauthorized());

        // New password works
        mvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        "john@citizen.za",
                                                        "password",
                                                        "NewUserPass1!"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /users/me/password returns 400 when current password is wrong")
    void changeOwnPassword_wrongCurrentPassword_returns401() throws Exception {
        mvc.perform(
                        patch("/v1/users/me/password")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "currentPassword",
                                                        "WrongPass1!",
                                                        "newPassword",
                                                        "NewUserPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    private String login(final String email, final String password) throws Exception {
        final MvcResult result =
                mvc.perform(
                                post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "email",
                                                                email,
                                                                "password",
                                                                password))))
                        .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
