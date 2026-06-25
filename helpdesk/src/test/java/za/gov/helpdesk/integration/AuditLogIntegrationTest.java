package za.gov.helpdesk.integration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.auditlog.repository.AuditLogRepository;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Audit Log Integration Tests")
public class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Autowired private AgentRepository agentRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private SlaPolicyRepository slaPolicyRepository;
    @Autowired private AuditLogRepository auditLogRepository; // or OutboxRepository

    private String adminToken;
    private String agentToken;
    private String userToken;
    private Long ticketId;
    private Long agentUserId;
    private Long agentId;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        ticketRepository.deleteAll();
        agentRepository.deleteAll();
        userRepository.deleteAll();

        if (slaPolicyRepository.count() == 0) {
            final var mediumPolicy = new SlaPolicy();
            mediumPolicy.setPriority(Ticket.Priority.MEDIUM);
            mediumPolicy.setResolutionMinutes(480);
            mediumPolicy.setResponseMinutes(480);
            slaPolicyRepository.save(mediumPolicy);
        }

        seedTestUsers();
        final User agentUser = createAgentUser("Jane Agent", "jane@gov.za", "AgentPass1!");
        agentUserId = agentUser.getId();

        final Agent agent =
                agentRepository.save(
                        Agent.builder()
                                .user(agentUser)
                                .availability(Agent.Availability.ONLINE)
                                .build());
        agentId = agent.getId();

        adminToken = login("admin@gov.za", "AdminPass1!");
        agentToken = login("jane@gov.za", "AgentPass1!");
        userToken = login("john@citizen.za", "UserPass1!");

        // Create a ticket so audit events exist for TICKET entity
        final String ticketBody =
                mvc.perform(
                                post("/v1/tickets")
                                        .header("Authorization", "Bearer " + userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                mapper.writeValueAsString(
                                                        Map.of(
                                                                "subject",
                                                                "Audit test ticket",
                                                                "description",
                                                                "Testing audit trail"))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        ticketId = mapper.readTree(ticketBody).get("id").asLong();
    }

    @Test
    @DisplayName("GET /v1/audit/tickets/{id} returns 200 for AGENT and ADMIN")
    void getTicketAuditLog_agentAndAdmin_returns200() throws Exception {
        mvc.perform(
                        get("/v1/audit/tickets/" + ticketId)
                                .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        mvc.perform(
                        get("/v1/audit/tickets/" + ticketId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /v1/audit/tickets/{id} returns 403 for USER role")
    void getTicketAuditLog_userRole_returns403() throws Exception {
        mvc.perform(
                        get("/v1/audit/tickets/" + ticketId)
                                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/audit/tickets/{id} returns 401 without token")
    void getTicketAuditLog_noToken_returns401() throws Exception {
        mvc.perform(get("/v1/audit/tickets/" + ticketId)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/audit/users/{id} returns 200 for ADMIN only")
    void getUserAuditLog_adminToken_returns200() throws Exception {
        mvc.perform(
                        get("/v1/audit/users/" + agentUserId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /v1/audit/users/{id} returns 403 for AGENT")
    void getUserAuditLog_agentToken_returns403() throws Exception {
        mvc.perform(
                        get("/v1/audit/users/" + agentUserId)
                                .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/audit/auth returns paginated auth events for ADMIN")
    void getAuthLogs_adminToken_returnsPaginatedEvents() throws Exception {
        await().atMost(7, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(
                        () -> {
                            mvc.perform(
                                            get("/v1/audit/auth")
                                                    .header(
                                                            "Authorization",
                                                            "Bearer " + adminToken))
                                    .andExpect(status().isOk())
                                    .andExpect(
                                            jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                                    .andExpect(jsonPath("$.content[0].action").isString());
                        });
    }

    @Test
    @DisplayName("GET /v1/audit/auth returns 403 for AGENT")
    void getAuthLogs_agentToken_returns403() throws Exception {
        mvc.perform(get("/v1/audit/auth").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/audit/tickets/{id} contains TICKET_CREATED entry after ticket creation")
    void getTicketAuditLog_afterCreation_containsCreatedEntry() throws Exception {
        await().atMost(7, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(
                        () -> {
                            mvc.perform(
                                            get("/v1/audit/tickets/" + ticketId)
                                                    .header(
                                                            "Authorization",
                                                            "Bearer " + agentToken))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$[0].action").value("TICKET_CREATED"))
                                    .andExpect(jsonPath("$[0].entityId").value(ticketId));
                        });
    }

    @Test
    @DisplayName("GET /v1/audit/tickets/{id} records STATUS_CHANGED after status change")
    void getTicketAuditLog_afterStatusChange_containsUpdatedEntry() throws Exception {
        // Step 1: Assign ticket using Admin authority layout

        mvc.perform(
                        patch("/v1/tickets/" + ticketId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(Map.of("assigneeId", agentId))))
                .andExpect(status().isOk());

        // Step 2: Change status using newly assigned Agent context
        await().atMost(7, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(
                        () -> {
                            mvc.perform(
                                            patch("/v1/tickets/" + ticketId)
                                                    .header("Authorization", "Bearer " + agentToken)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(
                                                            mapper.writeValueAsString(
                                                                    Map.of(
                                                                            "status",
                                                                            "IN_PROGRESS"))))
                                    .andExpect(status().isOk());
                        });

        // Step 3: Wait for async outbox thread loop to complete processing
        await().atMost(7, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(
                        () -> {
                            mvc.perform(
                                            get("/v1/audit/tickets/" + ticketId)
                                                    .header(
                                                            "Authorization",
                                                            "Bearer " + agentToken))
                                    .andExpect(status().isOk())
                                    .andExpect(
                                            jsonPath("$[?(@.action == 'STATUS_CHANGED')]")
                                                    .exists());
                        });
    }

    @Test
    @DisplayName("GET /v1/audit/actor/{actorId} returns entries for that actor")
    void getByActor_adminToken_returnsActorEntries() throws Exception {
        final String profileBody =
                mvc.perform(get("/v1/users/me").header("Authorization", "Bearer " + adminToken))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        final long adminId = mapper.readTree(profileBody).get("id").asLong();

        mvc.perform(
                        get("/v1/audit/actor/" + adminId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
