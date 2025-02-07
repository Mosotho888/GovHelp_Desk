package com.loggingsystem.springjwtauth.auth.service;

import com.loggingsystem.springjwtauth.auth.jwt.JwtUtil;
import com.loggingsystem.springjwtauth.auth.dto.JwtRequest;
import com.loggingsystem.springjwtauth.auth.dto.JwtResponse;
import com.loggingsystem.springjwtauth.employee.exception.UserAlreadyExistsException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class JwtService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmployeesRepository employeesRepository;

    public JwtService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, EmployeesRepository employeesRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.employeesRepository = employeesRepository;
    }

    public ResponseEntity<JwtResponse> generateToken (JwtRequest jwtRequest) {
        // This token is different that JWT
        log.info("Initiating token generation for user: {}", jwtRequest.getUserEmail());
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                jwtRequest.getUserEmail(), jwtRequest.getPassword()
        );

        // this will fault if credentials are not valid
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        log.info("Authentication successful for user: {}", jwtRequest.getUserEmail());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtToken = jwtUtil.generateToken((User) authentication.getPrincipal());
        log.info("JWT token generated successfully for user: {}", jwtRequest.getUserEmail());

        return ResponseEntity.ok(JwtResponse
                .builder()
                .token(jwtToken)
                .build());

    }

    /**
     * Creates a new employee. This method checks if the employee already exists by email,
     * encodes the password, and saves the employee to the repository.
     *
     * @param newEmployee the employee data to be used to create a new employee.
     * @return ResponseEntity with HTTP status CREATED.
     */
    public ResponseEntity<Void> registerEmployee(Employees newEmployee) {
        log.info("Attempting to create a new employee with email: {}", newEmployee.getEmail());
        checkWhetherEmployeeAlreadyExist(newEmployee);

        String encodedPassword = passwordEncoder.encode(newEmployee.getPassword());
        log.info("Password encoded successfully for email: {}", newEmployee.getEmail());

        newEmployee.setPassword(encodedPassword);
        newEmployee.setCreated_at(LocalDateTime.now());
        newEmployee.setRole("USER");

        Employees savedEmployee = employeesRepository.save(newEmployee);
        log.info("Employee with email {} saved successfully", savedEmployee.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Checks if an employee with the same email already exists in the system.
     *
     * @param newEmployee the request object containing employee details.
     * @throws UserAlreadyExistsException if the employee with the same email already exists.
     */
    private void checkWhetherEmployeeAlreadyExist(Employees newEmployee) {
        Boolean doesEmployeeExist = employeesRepository.existsByEmail(newEmployee.getEmail());

        if (doesEmployeeExist) {
            log.error("User with email {} already exists", newEmployee.getEmail());
            throw new UserAlreadyExistsException();
        }
    }
}
