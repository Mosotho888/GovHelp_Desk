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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Agent integration tests")
public class AgentIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String agentToken;
    private String userToken;
    private Long agentUserId;
    private Long agentId;

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

        final User agentUser =
                userRepository.save(
                        User.builder()
                                .name("Jane Agent")
                                .email("jane@gov.za")
                                .passwordHash(passwordEncoder.encode("AgentPass1!"))
                                .role(User.Role.AGENT)
                                .active(true)
                                .loginAttempts(0)
                                .timezone("Africa/Johannesburg")
                                .build());
        agentUserId = agentUser.getId();

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

        adminToken = login("admin@gov.za", "AdminPass1!");
        userToken = login("john@citizen.za", "UserPass1!");

        // Register Jane as an agent via the API
        final String body =
                mvc.perform(
                                post("/v1/agents")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "userId",
                                                                agentUserId,
                                                                "department",
                                                                "IT Support",
                                                                "availability",
                                                                "ONLINE"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        agentId = mapper.readTree(body).get("id").asLong();
        agentToken = login("jane@gov.za", "AgentPass1!");
    }

    @Test
    @DisplayName("POST /agents returns 201 for admin — agent registered with correct department")
    void createAgent_adminToken_returns201() throws Exception {
        final User newAgentUser =
                userRepository.save(
                        User.builder()
                                .name("Bob Agent")
                                .email("bob@gov.za")
                                .passwordHash(passwordEncoder.encode("AgentPass1!"))
                                .role(User.Role.AGENT)
                                .active(true)
                                .loginAttempts(0)
                                .timezone("Africa/Johannesburg")
                                .build());

        mvc.perform(
                        post("/v1/agents")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "userId",
                                                        newAgentUser.getId(),
                                                        "department",
                                                        "HR",
                                                        "availability",
                                                        "OFFLINE"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.department").value("HR"))
                .andExpect(jsonPath("$.availability").value("OFFLINE"))
                .andExpect(jsonPath("$.user.email").value("bob@gov.za"));
    }

    @Test
    @DisplayName("POST /agents returns 403 for non-admin")
    void createAgent_nonAdmin_returns403() throws Exception {
        mvc.perform(
                        post("/v1/agents")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(Map.of("userId", agentUserId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /agents returns 409 when user already registered as agent")
    void createAgent_duplicateUser_returns409() throws Exception {
        // agentUserId is already an agent from setUp()
        mvc.perform(
                        post("/v1/agents")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(Map.of("userId", agentUserId))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /agents returns paginated list for agent and admin roles")
    void getAllAgents_agentOrAdmin_returns200() throws Exception {
        mvc.perform(get("/v1/agents").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].user.email").value("jane@gov.za"));

        mvc.perform(get("/v1/agents").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /agents returns 403 for USER role")
    void getAllAgents_userRole_returns403() throws Exception {
        mvc.perform(get("/v1/agents").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /agents/{id} returns agent by ID")
    void getAgentById_existingId_returns200() throws Exception {
        mvc.perform(get("/v1/agents/" + agentId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agentId))
                .andExpect(jsonPath("$.user.email").value("jane@gov.za"));
    }

    @Test
    @DisplayName("GET /agents/{id} returns 404 for non-existent ID")
    void getAgentById_notFound_returns404() throws Exception {
        mvc.perform(get("/v1/agents/99999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /agents/{id} allows admin to update department and availability")
    void updateAgent_adminToken_returns200() throws Exception {
        mvc.perform(
                        patch("/v1/agents/" + agentId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "department",
                                                        "Finance",
                                                        "availability",
                                                        "BUSY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Finance"))
                .andExpect(jsonPath("$.availability").value("BUSY"));
    }

    @Test
    @DisplayName("PATCH /agents/{id} allows agent to update their own availability")
    void updateAgent_ownAgent_returns200() throws Exception {
        mvc.perform(
                        patch("/v1/agents/" + agentId)
                                .header("Authorization", "Bearer " + agentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(Map.of("availability", "AWAY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AWAY"));
    }

    @Test
    @DisplayName("GET /agents/{id}/stats returns ticket statistics for admin")
    void getAgentStats_adminToken_returns200() throws Exception {
        mvc.perform(
                        get("/v1/agents/" + agentId + "/stats")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").isNumber())
                .andExpect(jsonPath("$.resolvedCount").isNumber());
    }

    @Test
    @DisplayName("GET /agents/{id}/stats returns 403 for non-admin")
    void getAgentStats_nonAdmin_returns403() throws Exception {
        mvc.perform(
                        get("/v1/agents/" + agentId + "/stats")
                                .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
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
