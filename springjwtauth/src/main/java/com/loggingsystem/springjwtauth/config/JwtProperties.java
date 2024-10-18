package com.loggingsystem.springjwtauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "jwt")
@Configuration
@Data
public class JwtProperties {
    private String secretKey;
    private Long validity;
}
