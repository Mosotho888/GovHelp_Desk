package za.gov.helpdesk.api;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Black-box tests for {@code /v1/categories/**}. Run against a live instance via the {@code
 * api-tests} Maven profile - see {@link BaseApiTest}.
 */
@Tag("api")
@DisplayName("Category API black-box tests")
class CategoryApiIT extends BaseApiTest {

    @Test
    @DisplayName("GET /categories returns the tree with at least one root category")
    void fetchCategoryTree_anyAuthenticatedUser_returnsNonEmptyTree() {
        asUser().when()
                .get("/categories")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].id", notNullValue());
    }

    @Test
    @DisplayName("GET /categories without a token returns 401")
    void fetchCategoryTree_noToken_returns401() {
        given().when().get("/categories").then().statusCode(401);
    }

    @Test
    @DisplayName("POST /categories as a plain USER is forbidden (admin-only endpoint)")
    void createCategory_asPlainUser_returns403() {
        asUser().body(Map.of("name", "Should Not Be Created " + UUID.randomUUID()))
                .when()
                .post("/categories")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("POST /categories as an agent is forbidden (admin-only endpoint)")
    void createCategory_asAgent_returns403() {
        asAgent()
                .body(Map.of("name", "Should Not Be Created " + UUID.randomUUID()))
                .when()
                .post("/categories")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("POST /categories as an admin creates a new top-level category")
    void createCategory_asAdmin_succeeds() {
        final String uniqueName = "QA Test Category " + UUID.randomUUID();

        asAdmin()
                .body(Map.of("name", uniqueName))
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .body("name", equalTo(uniqueName))
                .body("active", equalTo(true))
                .body("level", equalTo(0));
    }

    @Test
    @DisplayName("POST /categories with a duplicate top-level name returns 409")
    void createCategory_duplicateTopLevelName_returns409() {
        final String uniqueName = "QA Duplicate Test " + UUID.randomUUID();

        asAdmin()
                .body(Map.of("name", uniqueName))
                .when()
                .post("/categories")
                .then()
                .statusCode(201);

        asAdmin()
                .body(Map.of("name", uniqueName))
                .when()
                .post("/categories")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("POST /categories with a blank name returns 400 validation error")
    void createCategory_blankName_returns400() {
        asAdmin()
                .body(Map.of("name", ""))
                .when()
                .post("/categories")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /categories/{id} for an unknown id returns 404")
    void fetchCategoryById_unknownId_returns404() {
        asUser().when().get("/categories/999999999").then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE /categories/{id} as a non-admin is forbidden")
    void deactivateCategory_asNonAdmin_returns403() {
        final int categoryId =
                asAdmin()
                        .body(Map.of("name", "QA Delete-RBAC Test " + UUID.randomUUID()))
                        .when()
                        .post("/categories")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asAgent().when().delete("/categories/" + categoryId).then().statusCode(403);
    }

    @Test
    @DisplayName("DELETE /categories/{id} as an admin deactivates a leaf category")
    void deactivateCategory_asAdminOnLeafCategory_succeeds() {
        final int categoryId =
                asAdmin()
                        .body(Map.of("name", "QA Deactivate Test " + UUID.randomUUID()))
                        .when()
                        .post("/categories")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("id");

        asAdmin().when().delete("/categories/" + categoryId).then().statusCode(204);

        asAdmin()
                .when()
                .get("/categories/" + categoryId)
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }
}
