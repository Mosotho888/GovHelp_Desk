package za.gov.helpdesk.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Security headers integration tests")
public class SecurityHeadersIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Response includes Strict-Transport-Security header")
    void response_includesHSTSHeader() throws Exception {
        mvc.perform(get("/v1/auth/login"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Strict-Transport-Security",
                        org.hamcrest.Matchers.containsString("max-age=")));
    }

    @Test
    @DisplayName("Response includes X-Frame-Options: DENY header")
    void response_includesXFrameOptionsHeader() throws Exception {
        mvc.perform(get("/v1/auth/login"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("Response includes Content-Security-Policy header")
    void response_includesCSPHeader() throws Exception {
        mvc.perform(get("/v1/auth/login"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 not 302 redirect")
    void unauthenticatedRequest_returns401NotRedirect() throws Exception {
        mvc.perform(get("/v1/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Error response never exposes stack trace")
    void errorResponse_doesNotExposeStackTrace() throws Exception {
        mvc.perform(get("/v1/tickets/999999")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("at za.gov.helpdesk")
                        )));
    }
}
