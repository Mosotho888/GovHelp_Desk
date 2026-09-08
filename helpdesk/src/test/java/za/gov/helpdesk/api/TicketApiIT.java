package za.gov.helpdesk.api;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;

/**
 * Black-box tests for {@code /v1/tickets/**}. Run against a live instance via the {@code api-tests}
 * Maven profile - see {@link BaseApiTest}.
 */
@Tag("api")
@DisplayName("Ticket API black-box tests")
class TicketApiIT extends BaseApiTest {

    @Test
    @DisplayName("GET /tickets without a token returns 401")
    void fetchTickets_noToken_returns401() {
        given().when().get("/tickets").then().statusCode(401);
    }

    @Test
    @DisplayName("POST /tickets creates a ticket and the response matches the ticket schema")
    void createTicket_validRequest_returnsSchemaValidTicket() {
        asUser().body(
                        Map.of(
                                "subject",
                                "Printer on 3rd floor is jammed",
                                "description",
                                "Paper jam, tried restarting."))
                .when()
                .post("/tickets")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body(
                        JsonSchemaValidator.matchesJsonSchemaInClasspath(
                                "schemas/ticket-response-schema.json"))
                .body("status", equalTo("OPEN"))
                .body("id", notNullValue());
    }

    @Test
    @DisplayName("POST /tickets with a blank subject returns 400 validation error")
    void createTicket_blankSubject_returns400() {
        asUser().body(Map.of("subject", "", "description", "Some description"))
                .when()
                .post("/tickets")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /tickets with no description field at all returns 400")
    void createTicket_missingDescription_returns400() {
        asUser().body(Map.of("subject", "Missing description test"))
                .when()
                .post("/tickets")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName(
            "GET /tickets/{id} for a ticket owned by a different user returns a disguised 404 (IDOR"
                    + " protection)")
    void fetchTicketById_ticketOwnedByAnotherUser_returnsDisguised404() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Private ticket for user-vs-user IDOR test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        // A second, unrelated end user must not be able to tell this ticket exists at all - the
        // API intentionally returns 404 rather than 403 here to avoid confirming its presence.
        asUser2().when().get("/tickets/" + ticketId).then().statusCode(404);

        // The actual owner can still read it back.
        asUser().when().get("/tickets/" + ticketId).then().statusCode(200);
    }

    @Test
    @DisplayName(
            "GET /tickets/{id} for a ticket assigned to a different agent returns a disguised 404"
                    + " (IDOR protection)")
    void fetchTicketById_ticketAssignedToAnotherAgent_returnsDisguised404() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Ticket for agent-vs-agent IDOR test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        // Seeded agent id 1 is Thabo Mokoena (agentEmail); assign the ticket to him explicitly.
        asAdmin()
                .body(Map.of("assigneeId", 1))
                .when()
                .patch("/tickets/" + ticketId)
                .then()
                .statusCode(200);

        // A different agent (Sarah Jenkins, seeded agent id 2) who is not the assignee must not
        // be able to see it - unassigned tickets are visible to any agent by design, but an
        // assigned ticket is only visible to its own assignee (or an admin).
        asAgent2().when().get("/tickets/" + ticketId).then().statusCode(404);

        // The actual assignee can still read it back.
        asAgent().when().get("/tickets/" + ticketId).then().statusCode(200);
    }

    @Test
    @DisplayName("GET /tickets/{id} for an unknown id returns 404")
    void fetchTicketById_unknownId_returns404() {
        asUser().when().get("/tickets/999999999").then().statusCode(404);
    }

    @Test
    @DisplayName("PATCH /tickets/{id} as a plain USER is forbidden (agent/admin only endpoint)")
    void updateTicket_asPlainUser_returns403() {
        final int ticketId =
                asUser().body(Map.of("subject", "Ticket for RBAC test", "description", "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asUser().body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/tickets/" + ticketId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName(
            "PATCH /tickets/{id} as an agent can move status forward through a legal transition")
    void updateTicket_asAgentLegalTransition_succeeds() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Ticket for legal transition test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asAgent()
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/tickets/" + ticketId)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /tickets/{id} rejects an illegal status transition with 422")
    void updateTicket_illegalTransition_returns422() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Ticket for illegal transition test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        // OPEN -> RESOLVED is not a legal direct transition per TicketStatusTransitionPolicy.
        asAgent()
                .body(Map.of("status", "RESOLVED"))
                .when()
                .patch("/tickets/" + ticketId)
                .then()
                .statusCode(422);
    }

    @Test
    @DisplayName("DELETE /tickets/{id} as a non-admin is forbidden")
    void deleteTicket_asNonAdmin_returns403() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Ticket for delete-RBAC test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asAgent().when().delete("/tickets/" + ticketId).then().statusCode(403);
    }

    @Test
    @DisplayName("DELETE /tickets/{id} as an admin succeeds and the ticket is then a 404")
    void deleteTicket_asAdmin_succeedsAndTicketBecomesUnreachable() {
        final int ticketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "Ticket for delete-success test",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asAdmin().when().delete("/tickets/" + ticketId).then().statusCode(204);
        asAdmin().when().get("/tickets/" + ticketId).then().statusCode(404);
    }

    @Test
    @DisplayName("GET /tickets respects the page size parameter")
    void fetchTickets_pageSizeParameter_isRespected() {
        asAgent()
                .queryParam("size", 5)
                .when()
                .get("/tickets")
                .then()
                .statusCode(200)
                .body("size", equalTo(5))
                .body("content.size()", lessThanOrEqualTo(5));
    }

    @Test
    @DisplayName("GET /tickets filtered by status only returns tickets in that status")
    void fetchTickets_filteredByStatus_onlyReturnsMatchingStatus() {
        asAgent()
                .queryParam("status", "OPEN")
                .queryParam("size", 50)
                .when()
                .get("/tickets")
                .then()
                .statusCode(200)
                .body("content.findAll { it.status != 'OPEN' }.size()", equalTo(0));
    }

    @Test
    @DisplayName("GET /tickets with an invalid status enum value returns 400, not 500")
    void fetchTickets_invalidStatusValue_returns400() {
        asAgent()
                .queryParam("status", "NOT_A_REAL_STATUS")
                .when()
                .get("/tickets")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("A regular USER only sees their own tickets in the list, never anyone else's")
    void fetchTickets_asUser_onlySeesOwnTickets() {
        final int myTicketId =
                asUser().body(
                                Map.of(
                                        "subject",
                                        "My own visibility-check ticket",
                                        "description",
                                        "..."))
                        .when()
                        .post("/tickets")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asUser().queryParam("size", 100)
                .when()
                .get("/tickets")
                .then()
                .statusCode(200)
                .body("content.find { it.id == " + myTicketId + " }", notNullValue())
                .body(
                        "content.requester.id.unique()",
                        org.hamcrest.Matchers.hasSize(lessThanOrEqualTo(1)));
    }
}
