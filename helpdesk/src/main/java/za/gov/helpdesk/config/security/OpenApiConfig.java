package za.gov.helpdesk.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuration component responsible for exposing and customising the OpenAPI v3 specifications.
 * Defines metadata documentation parameters, contact channels, and configures the global JSON Web
 * Token (JWT) Bearer authentication security schemes for Swagger UI testing.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Instantiates and configures a custom {@link OpenAPI} specification model definition bean.
     * Hooks up API documentation headers, default IT support contact metadata details, and a
     * unified, re-usable "bearerAuth" HTTP Bearer schema boundary requirement.
     *
     * @return a pre-populated {@link OpenAPI} metadata container definition
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(
                        new Server().url("https://api.sothoman.com").description("Production"))
                .addServersItem(new Server().url("http://localhost:8080").description("Local"))
                .info(
                        new Info()
                                .title("Helpdesk API")
                                .version("1.0.0")
                                .description("Internal API for the ZA Government Helpdesk System")
                                .contact(
                                        new Contact()
                                                .name("IT Support")
                                                .email("support@helpdesk.gov.za")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .name("Authorization")
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
