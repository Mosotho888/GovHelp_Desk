package za.gov.helpdesk.config.messaging;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import lombok.RequiredArgsConstructor;

/**
 * Configuration component responsible for instantiating and configuring SMTP mail integration
 * infrastructure. Binds environment application properties to establish secure connection
 * parameters used by the notification engine.
 */
@Configuration
@RequiredArgsConstructor
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    /**
     * Instantiates and customizes a {@link JavaMailSender} bean implementation configured for
     * Simple Mail Transfer Protocol (SMTP) communication. Activates essential transport layer
     * security attributes including authentication challenges and explicit STARTTLS negotiation
     * upgrades.
     *
     * @return a fully pre-configured {@link JavaMailSender} engine instance
     */
    @Bean
    public JavaMailSender javaMailSender() {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        final Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
