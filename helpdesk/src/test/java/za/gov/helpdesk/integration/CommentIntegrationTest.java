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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Comment integration tests")
public class CommentIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired
    private SlaPolicyRepository slaPolicyRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private String userToken;
    private String agentToken;
    private long ticketId;
    private Agent agent;

    @BeforeEach
    void setUp() throws Exception {

        agentRepository.deleteAll();
        slaPolicyRepository.deleteAll();
        userRepository.deleteAll();
        slaPolicyRepository.save(SlaPolicy.builder()
                .priority(Ticket.Priority.MEDIUM)
                .resolutionMinutes(480) // Example threshold metric
                .responseMinutes(480)
                .build());
        User citizen = userRepository.save(User.builder()
                .name("John Public").email("john@citizen.za")
                .passwordHash(passwordEncoder.encode("UserPass1!"))
                .role(User.Role.USER).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());

        User agentUser = userRepository.save(User.builder()
                .name("Jane Agent").email("jane@gov.za")
                .passwordHash(passwordEncoder.encode("AgentPass1!"))
                .role(User.Role.AGENT).active(true).loginAttempts(0)
                .timezone("Africa/Johannesburg").build());

        agent = agentRepository.save(Agent.builder()
                .user(agentUser)
                .availability(Agent.Availability.ONLINE)
                .build());

        userToken  = login("john@citizen.za", "UserPass1!");
        agentToken = login("jane@gov.za",     "AgentPass1!");

        ticketId = createTicket(userToken, "Printer broken", "Won't print");
    }

    @Test
    @DisplayName("POST /tickets/{id}/comments returns 201 with comment body")
    void addComment_validRequest_returns201() throws Exception {
        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Any update on this?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.body").value("Any update on this?"))
                .andExpect(jsonPath("$.author.email").value("john@citizen.za"))
                .andExpect(jsonPath("$.internal").value(false));
    }

    @Test
    @DisplayName("POST /tickets/{id}/comments returns 401 without token")
    void addComment_noToken_returns401() throws Exception {
        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Unauthenticated"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /tickets/{id}/comments returns 400 for blank body")
    void addComment_blankBody_returns400() throws Exception {
        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /tickets/{id}/comments returns 404 for non-existent ticket")
    void addComment_ticketNotFound_returns404() throws Exception {
        mvc.perform(post("/v1/tickets/99999/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Lost ticket comment"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Agent can post internal note — USER cannot see it")
    void addComment_agentInternalNote_hiddenFromUser() throws Exception {
        // Agent posts an internal note
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        ticket.setAssignee(agent);
        ticketRepository.saveAndFlush(ticket);
        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "body", "Escalating internally — do not share",
                                "internal", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(true));

        // Also post a public comment so user gets at least one visible comment
        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "body", "We are looking into this",
                                "internal", false
                        ))))
                .andExpect(status().isCreated());

        // USER listing comments must NOT include the internal note
        mvc.perform(get("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].internal", everyItem(is(false))))
                .andExpect(jsonPath("$.content[*].body",
                        not(hasItem("Escalating internally — do not share"))));

        // Agent listing can see the internal note
        mvc.perform(get("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].body",
                        hasItem("Escalating internally — do not share")));
    }

    @Test
    @DisplayName("GET /tickets/{id}/comments returns paginated comments in order")
    void getComments_multiplePosts_returnsPaginatedAscending() throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        ticket.setAssignee(agent);
        ticketRepository.saveAndFlush(ticket);

        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "First comment"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Agent response"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].body").value("First comment"))
                .andExpect(jsonPath("$.content[1].body").value("Agent response"));
    }

    @Test
    @DisplayName("POST /comments/{id}/replies returns 201 with parent reference")
    void addReply_validRequest_returns201() throws Exception {
        // Create parent comment
        String commentBody = mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Parent comment"))))
                .andReturn().getResponse().getContentAsString();

        long commentId = mapper.readTree(commentBody).get("id").asLong();

        mvc.perform(post("/v1/comments/" + commentId + "/replies")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "We are on it"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("We are on it"))
                .andExpect(jsonPath("$.author.email").value("jane@gov.za"));
    }

    @Test
    @DisplayName("GET /comments/{id}/replies returns 200 with all replies")
    void getReplies_hasReplies_returnsAll() throws Exception {
        String commentBody = mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Original comment"))))
                .andReturn().getResponse().getContentAsString();

        long commentId = mapper.readTree(commentBody).get("id").asLong();

        mvc.perform(post("/v1/comments/" + commentId + "/replies")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Reply one"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/comments/" + commentId + "/replies")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Reply two"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/comments/" + commentId + "/replies")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("PUT /comments/{id} allows author to edit within 15-minute window")
    void updateComment_byAuthorWithinWindow_returns200() throws Exception {
        String commentBody = mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Original text"))))
                .andReturn().getResponse().getContentAsString();

        long commentId = mapper.readTree(commentBody).get("id").asLong();

        mvc.perform(put("/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Corrected text"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Corrected text"));
    }

    @Test
    @DisplayName("PUT /comments/{id} returns 403 when non-author (non-admin) tries to edit")
    void updateComment_byOtherUser_returns403() throws Exception {
        String commentBody = mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "User's comment"))))
                .andReturn().getResponse().getContentAsString();

        long commentId = mapper.readTree(commentBody).get("id").asLong();

        mvc.perform(put("/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "Agent tampers"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /comments/{id} allows author to delete within 15-minute window")
    void deleteComment_byAuthorWithinWindow_returns204() throws Exception {
        String commentBody = mvc.perform(post("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("body", "To be deleted"))))
                .andReturn().getResponse().getContentAsString();

        long commentId = mapper.readTree(commentBody).get("id").asLong();

        mvc.perform(delete("/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Comment should no longer appear in list
        mvc.perform(get("/v1/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createTicket(String token, String subject, String description) throws Exception {
        String body = mvc.perform(post("/v1/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "subject", subject,
                                "description", description
                        ))))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
}
