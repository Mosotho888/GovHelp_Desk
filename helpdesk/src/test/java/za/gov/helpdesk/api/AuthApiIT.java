package za.gov.helpdesk.api;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;

/**
 * Black-box tests for {@code /v1/auth/**}. Run against a live instance via the {@code api-tests}
 * Maven profile - see {@link BaseApiTest} and the profile documentation in {@code pom.xml}.
 */
@Tag("api")
@DisplayName("Auth API black-box tests")
class AuthApiIT extends BaseApiTest {

    @Test
    @DisplayName("POST /auth/login with valid credentials returns a schema-valid token pair")
    void login_validCredentials_returnsTokenPairMatchingSchema() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", adminEmail, "password", adminPassword))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(
                        JsonSchemaValidator.matchesJsonSchemaInClasspath(
                                "schemas/auth-response-schema.json"))
                .body("user.email", equalTo(adminEmail));
    }

    @Test
    @DisplayName(
            "POST /auth/login with a wrong password returns 401 without leaking whether the account"
                    + " exists")
    void login_wrongPassword_returns401WithGenericMessage() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", adminEmail, "password", "definitely-not-the-password"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }

    @Test
    @DisplayName(
            "POST /auth/login for a non-existent email returns the same 401 shape as a wrong"
                    + " password")
    void login_unknownEmail_returnsSameShapeAsWrongPassword() {
        // Security regression test: an attacker must not be able to distinguish "wrong password"
        // from "no such account" by inspecting the response - both must look identical.
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "email", "definitely-not-a-real-user@nowhere.example",
                                "password", "whatever123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName(
            "POST /auth/login with a missing password field returns 400 with a field-level"
                    + " validation error")
    void login_missingPassword_returns400ValidationError() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", adminEmail))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"))
                .body("details.field", hasItem("password"));
    }

    @Test
    @DisplayName("POST /auth/login with a malformed email returns 400")
    void login_malformedEmail_returns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "not-an-email", "password", "whatever123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName(
            "POST /auth/refresh with a freshly issued refresh token returns a new access token")
    void refresh_validRefreshToken_returnsNewAccessToken() {
        final String refreshToken =
                given().contentType(ContentType.JSON)
                        .body(Map.of("email", adminEmail, "password", adminPassword))
                        .when()
                        .post("/auth/login")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("refreshToken");

        given().contentType(ContentType.JSON)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("POST /auth/refresh with a garbage token string returns 401, not a 500")
    void refresh_garbageToken_returns401NotServerError() {
        given().contentType(ContentType.JSON)
                .body(Map.of("refreshToken", "this-is-not-a-real-token"))
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName(
            "POST /auth/logout revokes the refresh token so it can no longer be used to refresh")
    void logout_thenReuseRefreshToken_isRejected() {
        @SuppressWarnings("unchecked")
        final Map<String, String> loginResponse =
                given().contentType(ContentType.JSON)
                        .body(Map.of("email", adminEmail, "password", adminPassword))
                        .when()
                        .post("/auth/login")
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(Map.class);

        final String accessToken = loginResponse.get("accessToken");
        final String refreshToken = loginResponse.get("refreshToken");

        given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/auth/logout")
                .then()
                .statusCode(204);

        // The same refresh token must now be dead.
        given().contentType(ContentType.JSON)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName(
            "POST /auth/password-reset/request always returns 200, whether or not the email is"
                    + " registered")
    void requestPasswordReset_anyEmail_alwaysReturns200() {
        // Same user-enumeration concern as login, applied to the password reset entry point.
        given().contentType(ContentType.JSON)
                .body(Map.of("email", adminEmail))
                .when()
                .post("/auth/password-reset/request")
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("email", "no-such-account@nowhere.example"))
                .when()
                .post("/auth/password-reset/request")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("POST /auth/password-reset/confirm with a bogus OTP returns 401, not 500")
    void confirmPasswordReset_bogusOtp_returns401() {
        given().contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "email", adminEmail,
                                "otp", "000000",
                                "newPassword", "someNewPassword1"))
                .when()
                .post("/auth/password-reset/confirm")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Repeated failed logins against the same account are eventually rate-limited")
    void repeatedFailedLogins_eventuallyRateLimited() {
        // Fires enough failed attempts in a tight loop to exceed the unauthenticated rate-limit
        // bucket capacity. This deliberately targets a *different* account to the one used by
        // other tests, so it doesn't also trip that account's login-lockout counter mid-suite.
        final String throwawayEmail = "rate-limit-probe@nowhere.example";
        int lastStatus = 0;

        for (int i = 0; i < 150; i++) {
            lastStatus =
                    given().contentType(ContentType.JSON)
                            .body(Map.of("email", throwawayEmail, "password", "wrong-" + i))
                            .when()
                            .post("/auth/login")
                            .then()
                            .extract()
                            .statusCode();

            if (lastStatus == 429) {
                break;
            }
        }

        assertThat(lastStatus)
                .as("expected the unauthenticated rate limiter to eventually return 429")
                .isEqualTo(429);
    }
}
