package za.gov.helpdesk.api;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Base class for the black-box API test suite.
 *
 * <p>Unlike the {@code unit} and {@code integration} test packages elsewhere in this module,
 * classes under {@code za.gov.helpdesk.api} treat the application as an opaque HTTP service -
 * exactly how a QA engineer or an external API consumer would test it. They never touch Spring
 * beans, the persistence layer, or any internal class directly; every interaction goes over real
 * HTTP against whatever {@code api.baseUri} points at.
 *
 * <p>This is what makes the suite portable across environments: the same test classes can run
 * against a local {@code docker compose} stack, a CI-provisioned instance, or a public staging /
 * demo deployment, just by changing the {@code -D} properties below. See the {@code api-tests}
 * Maven profile in {@code pom.xml} for how this is wired up.
 */
@ActiveProfiles("test")
@SuppressWarnings("PMD.MutableStaticState")
public abstract class BaseApiTest {

    protected static String adminEmail;
    protected static String adminPassword;
    protected static String agentEmail;
    protected static String agentPassword;
    protected static String agent2Email;
    protected static String agent2Password;
    protected static String userEmail;
    protected static String userPassword;
    protected static String user2Email;
    protected static String user2Password;

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = System.getProperty("api.baseUri", "http://localhost:8080");
        RestAssured.basePath = "/v1";

        adminEmail = System.getProperty("api.admin.email", "admin@helpdesk.gov.za");
        adminPassword = System.getProperty("api.admin.password", "changeme");
        // Two distinct seeded agents (Thabo Mokoena / Sarah Jenkins) are needed to test that one
        // agent cannot read a ticket assigned to the other (IDOR protection between agents).
        agentEmail = System.getProperty("api.agent.email", "thabo.m@company.co.za");
        agentPassword = System.getProperty("api.agent.password", "changeme");
        agent2Email = System.getProperty("api.agent2.email", "s.jenkins@it.com");
        agent2Password = System.getProperty("api.agent2.password", "changeme");
        // Two distinct seeded end users (Lerato Dlamini / Mark Thompson) are needed to test that
        // one user cannot read another user's own ticket.
        userEmail = System.getProperty("api.user.email", "lerato.d@client.org");
        userPassword = System.getProperty("api.user.password", "changeme");
        user2Email = System.getProperty("api.user2.email", "m.thompson@gmail.com");
        user2Password = System.getProperty("api.user2.password", "changeme");
    }

    /**
     * Logs in with the given credentials and returns the raw access token string, ready to be
     * dropped straight into an {@code Authorization: Bearer <token>} header by a calling test.
     *
     * <p>Deliberately re-authenticates on every call rather than caching a token across tests:
     * black-box tests should never assume execution order or share mutable state between each
     * other, since a real QA suite must tolerate being run in any order, or as a single test in
     * isolation, or in parallel.
     *
     * @param email the account email to authenticate as
     * @param password the account password to authenticate with
     * @return the freshly issued JWT access token
     */
    protected static String accessTokenFor(final String email, final String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    /** Convenience for building an already-authenticated request as the seeded admin account. */
    protected static RequestSpecification asAdmin() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessTokenFor(adminEmail, adminPassword));
    }

    /** Convenience for building an already-authenticated request as the seeded agent account. */
    protected static RequestSpecification asAgent() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessTokenFor(agentEmail, agentPassword));
    }

    /**
     * Convenience for building an already-authenticated request as a second, distinct seeded agent
     * account - needed to test that one agent cannot read a ticket assigned to another.
     */
    protected static RequestSpecification asAgent2() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessTokenFor(agent2Email, agent2Password));
    }

    /** Convenience for building an already-authenticated request as the seeded end-user account. */
    protected static RequestSpecification asUser() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessTokenFor(userEmail, userPassword));
    }

    /**
     * Convenience for building an already-authenticated request as a second, distinct seeded
     * end-user account - needed to test that one user cannot read another user's own ticket.
     */
    protected static RequestSpecification asUser2() {
        return given().contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessTokenFor(user2Email, user2Password));
    }
}
