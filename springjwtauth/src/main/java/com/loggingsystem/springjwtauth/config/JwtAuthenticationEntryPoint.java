package com.loggingsystem.springjwtauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        Exception exception = (Exception) request.getAttribute("exception");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON_VALUE);

        log.error("Authentication Exception: {}", exception, exception);

        Map<String, Object> data = new HashMap<>();
        data.put("message", exception != null ? exception.getMessage() : authException.getClass().toString());
        OutputStream outputStream = response.getOutputStream();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(outputStream, data);
        outputStream.flush();
    }
}
