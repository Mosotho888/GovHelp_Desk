package za.gov.helpdesk.integration;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("helpdesk_test")
                    .withUsername("helpdesk")
                    .withPassword("helpdesk");

    static final RabbitMQContainer RABBIT_MQ_CONTAINER =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        POSTGRE_SQL_CONTAINER.start();
        RABBIT_MQ_CONTAINER.start();
    }

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected SlaPolicyRepository slaPolicyRepository;

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add(
                "spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.rabbitmq.host", RABBIT_MQ_CONTAINER::getHost);
        registry.add("spring.rabbitmq.port", RABBIT_MQ_CONTAINER::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT_MQ_CONTAINER::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT_MQ_CONTAINER::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> "false");
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE outbox_events, audit_log, attachments, comments, ticket_sla,"
                        + " refresh_tokens, password_reset_tokens, tickets, agents, users RESTART"
                        + " IDENTITY CASCADE;");
    }

    protected String login(final String email, final String password) throws Exception {
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

    protected void seedTestUsers() {
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
    }

    protected User createAgentUser(String name, String email, String password) {
        return userRepository.save(
                User.builder()
                        .name(name)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(User.Role.AGENT)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());
    }

    protected void seedCoreUsersAndSla() {
        slaPolicyRepository.save(
                SlaPolicy.builder()
                        .priority(Ticket.Priority.MEDIUM)
                        .resolutionMinutes(480)
                        .responseMinutes(480)
                        .build());

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
    }

    protected User saveAgentUser() {
        return userRepository.save(
                User.builder()
                        .name("Jane Agent")
                        .email("jane@gov.za")
                        .passwordHash(passwordEncoder.encode("AgentPass1!"))
                        .role(User.Role.AGENT)
                        .active(true)
                        .loginAttempts(0)
                        .timezone("Africa/Johannesburg")
                        .build());
    }
}
