package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.jwtUtil.JwtCreator;
//import com.loggingsystem.springjwtauth.jwtUtil.JwtHelper;
import com.loggingsystem.springjwtauth.model.JwtRequest;
import com.loggingsystem.springjwtauth.model.JwtResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class JwtService {
    private final AuthenticationManager authenticationManager;
    private final JwtCreator jwtCreator;

    public JwtService(AuthenticationManager authenticationManager, JwtCreator jwtCreator) {
        this.authenticationManager = authenticationManager;
        this.jwtCreator = jwtCreator;
    }

    public ResponseEntity<JwtResponse> generateToken (JwtRequest jwtRequest) {
        // This token is different that JWT
        log.info("Initiating token generation for user: {}", jwtRequest.getUseremail());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                jwtRequest.getUseremail(), jwtRequest.getPassword()
        );

        try {
            // this will fault if credentials are not valid
            Authentication authentication = authenticationManager.authenticate(token);
            log.info("Authentication successful for user: {}", jwtRequest.getUseremail());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwtToken = jwtCreator.generateToken((User) authentication.getPrincipal());
            log.info("JWT token generated successfully for user: {}", jwtRequest.getUseremail());

            return ResponseEntity.ok(JwtResponse
                    .builder()
                    .token(jwtToken)
                    .build());
        } catch (BadCredentialsException exception) {
            log.error("Authentication failed for user: {}. Invalid credentials provided.", jwtRequest.getUseremail());
            throw exception; // Propagate the exception for handling at a higher level
        } catch (Exception exception) {
            log.error("An unexpected error occurred during token generation for user: {}: {}",
                    jwtRequest.getUseremail(), exception.getMessage());
            throw exception; // Propagate the exception for handling at a higher level
        }
    }
}
