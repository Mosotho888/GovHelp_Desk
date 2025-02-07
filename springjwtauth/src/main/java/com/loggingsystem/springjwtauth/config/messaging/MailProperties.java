package com.loggingsystem.springjwtauth.config.messaging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration // Mark as a configuration class
@ConfigurationProperties(prefix = "spring.mail") // Prefix from application.properties
@Data
public class MailProperties {

    private String host;
    private int port;
    private String username;
    private String password;

}
