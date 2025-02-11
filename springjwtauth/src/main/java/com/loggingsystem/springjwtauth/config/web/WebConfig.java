package com.loggingsystem.springjwtauth.config.web;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**")  // Adjust the path to match your API endpoints
                        .allowedOrigins("http://localhost:3000", "https://govhelpdesk-production.up.railway.app/")  // Allow frontend origin
                        .allowedMethods("*")  // Allowed HTTP methods
                        .allowedHeaders("*")  // Allow all headers
                        .allowCredentials(true)
                        .maxAge(Duration.ofMinutes(5L).toMinutes());  // Allow credentials (cookies, authorization headers, etc.)
            }
        };
    }
}
