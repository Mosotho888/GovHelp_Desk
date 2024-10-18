package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.jwtUtil.JwtHelper;
import com.loggingsystem.springjwtauth.model.JwtRequest;
import com.loggingsystem.springjwtauth.model.JwtResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class JwtService {
    private final AuthenticationManager authenticationManager;
    private final EmployeeUserDetailsService employeeUserDetailsService;
    private final JwtHelper jwtHelper;

    public JwtService(AuthenticationManager authenticationManager, EmployeeUserDetailsService employeeUserDetailsService, JwtHelper jwtHelper) {
        this.authenticationManager = authenticationManager;
        this.employeeUserDetailsService = employeeUserDetailsService;
        this.jwtHelper = jwtHelper;
    }

    public ResponseEntity<JwtResponse> generateToken (JwtRequest jwtRequest) {
        try {

            this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.getUseremail(), jwtRequest.getPassword()));
            UserDetails userDetails = employeeUserDetailsService.loadUserByUsername(jwtRequest.getUseremail());
            String token = jwtHelper.createToken(Collections.emptyMap(), userDetails.getUsername());

            log.info("token: {}", token);

            return ResponseEntity.ok(JwtResponse.builder()
                    .token(token)
                    .build());
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }
}
