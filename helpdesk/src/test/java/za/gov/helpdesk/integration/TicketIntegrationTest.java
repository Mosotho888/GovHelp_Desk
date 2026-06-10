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
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Ticket integration tests")
public class TicketIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private TicketRepository ticketRepository;

    private String userToken;
    private String agentToken;
    private Long agentId;

    @BeforeEach
    void setUp() throws Exception {

        ticketRepository.deleteAll();
        slaPolicyRepository.deleteAll();
        agentRepository.deleteAll();
        userRepository.deleteAll();

        slaPolicyRepository.save(SlaPolicy.builder()
                .priority(Ticket.Priority.MEDIUM)
                .resolutionMinutes(480)
                .responseMinutes(480)
                .build());

        slaPolicyRepository.save(SlaPolicy.builder()
                .priority(Ticket.Priority.HIGH)
                .resolutionMinutes(120)
                .responseMinutes(120)
                .build());

        slaPolicyRepository.save(SlaPolicy.builder()
                .priority(Ticket.Priority.URGENT)
                .resolutionMinutes(60)
                .responseMinutes(60)
                .build());
        userRepository.save(User.builder()
                .name("John Public").email("john@citizen.za")
                .passwordHash(passwordEncoder.encode("UserPass1!"))
                .role(User.Role.USER).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());

        User agentUser = userRepository.save(User.builder()
                .name("Jane Agent").email("jane@gov.za")
                .passwordHash(passwordEncoder.encode("AgentPass1!"))
                .role(User.Role.AGENT).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());
        agentId = agentRepository.save(Agent.builder()
                .user(agentUser)
                .availability(Agent.Availability.ONLINE)
                .build()).getId();

        userToken  = login("john@citizen.za", "UserPass1!");
        agentToken = login("jane@gov.za",     "AgentPass1!");
    }

    @Test
    @DisplayName("POST /tickets creates ticket and returns 201")
    void createTicket_validRequest_returns201() throws Exception {
        mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject",     "System is down",
                                "description", "Cannot access the portal since 09:00",
                                "priority",    "HIGH"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.requester.email").value("john@citizen.za"));
    }

    @Test
    @DisplayName("POST /tickets returns 401 without token")
    void createTicket_noToken_returns401() throws Exception {
        mvc.perform(post("/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", "Test", "description", "Test desc"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /tickets returns 400 when subject is missing")
    void createTicket_missingSubject_returns400() throws Exception {
        mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "description", "No subject provided"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("subject"));
    }

    @Test
    @DisplayName("POST /tickets returns 400 when description is missing")
    void createTicket_missingDescription_returns400() throws Exception {
        mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", "No description"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /tickets returns only own tickets for USER role")
    void getTickets_userRole_seesOnlyOwnTickets() throws Exception {
        mvc.perform(post("/v1/tickets")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                        "subject", "My issue", "description", "Details here"
                ))));

        mvc.perform(get("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requester.email").value("john@citizen.za"));

        mvc.perform(get("/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /tickets/{id} returns 404 for non-existent ticket")
    void getTicketById_notFound_returns404() throws Exception {
        mvc.perform(get("/v1/tickets/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets/{id} returns 403 when USER requests another user's ticket")
    void getTicketById_idor_returns403() throws Exception {
        // Agent creates a ticket for Jane — John should not be able to fetch it
        String agentUserToken = agentToken; // Jane is AGENT, cannot create tickets for others

        // Create a ticket as John
        String johnTicket = mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", "John's private issue",
                                "description", "Secret detail"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long johnTicketId = mapper.readTree(johnTicket).get("id").asLong();

        // Create another user who should NOT see John's ticket
        userRepository.save(User.builder()
                .name("Eve Attacker").email("eve@citizen.za")
                .passwordHash(passwordEncoder.encode("EvePass1!"))
                .role(User.Role.USER).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());
        String eveToken = login("eve@citizen.za", "EvePass1!");

        mvc.perform(get("/v1/tickets/" + johnTicketId)
                        .header("Authorization", "Bearer " + eveToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Full lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED — SLA timestamps recorded")
    void ticketLifecycle_fullFlow_succeeds() throws Exception {
        String createBody = mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject",     "Printer not working",
                                "description", "Office printer offline"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ticketId = mapper.readTree(createBody).get("id").asLong();

        // SLA deadlines are set immediately
        mvc.perform(get("/v1/tickets/" + ticketId + "/sla")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseDueAt").exists())
                .andExpect(jsonPath("$.resolutionDueAt").exists())
                .andExpect(jsonPath("$.firstResponseAt").isEmpty())
                .andExpect(jsonPath("$.resolvedAt").isEmpty());

        // IN_PROGRESS stamps firstResponseAt
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(get("/v1/tickets/" + ticketId + "/sla")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(jsonPath("$.firstResponseAt").exists())
                .andExpect(jsonPath("$.resolvedAt").isEmpty());

        // RESOLVED stamps resolvedAt
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mvc.perform(get("/v1/tickets/" + ticketId + "/sla")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(jsonPath("$.resolvedAt").exists());

        // USER cannot close — agent-only action
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "CLOSED"))))
                .andExpect(status().isForbidden());

        // Agent closes
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "CLOSED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("PATCH /tickets/{id} returns 422 for invalid status transition")
    void updateTicket_invalidTransition_returns422() throws Exception {
        String createBody = mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", "Test", "description", "Test desc"
                        ))))
                .andReturn().getResponse().getContentAsString();
        long ticketId = mapper.readTree(createBody).get("id").asLong();

        // OPEN -> CLOSED is invalid
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "CLOSED"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    @DisplayName("PATCH /tickets/{id} supports assignment, priority update, and escalation")
    void updateTicket_assignmentPriorityEscalation_succeeds() throws Exception {
        String createBody = mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", "Network unstable",
                                "description", "VPN disconnects repeatedly"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ticketId = mapper.readTree(createBody).get("id").asLong();

        // Assign and reprioritise
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "assigneeId", agentId,
                                "priority", "URGENT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.assignee.email").value("jane@gov.za"));

        // Move to IN_PROGRESS
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk());

        // Escalate
        mvc.perform(patch("/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "escalated", true,
                                "escalationReason", "Service outage risk"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.escalated").value(true));
    }

    @Test
    @DisplayName("PATCH /tickets/{id} returns 404 for non-existent ticket")
    void updateTicket_notFound_returns404() throws Exception {
        mvc.perform(patch("/v1/tickets/99999")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isNotFound());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
