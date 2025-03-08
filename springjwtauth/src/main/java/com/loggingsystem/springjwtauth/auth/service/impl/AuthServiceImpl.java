package com.loggingsystem.springjwtauth.auth.service.impl;

import com.loggingsystem.springjwtauth.auth.dto.LoginResponse;
import com.loggingsystem.springjwtauth.auth.dto.RegisterRequest;
import com.loggingsystem.springjwtauth.auth.jwt.JwtUtil;
import com.loggingsystem.springjwtauth.auth.dto.LoginRequest;
import com.loggingsystem.springjwtauth.auth.service.AuthenticationService;
import com.loggingsystem.springjwtauth.auth.service.RegisterRequestConverter;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.employee.exception.UserAlreadyExistsException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import com.loggingsystem.springjwtauth.employee.service.EmployeeToEmployeeResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class AuthServiceImpl implements AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RegisterRequestConverter registerRequestConverter;
    private final EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter;
    private final EmployeesRepository employeesRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtUtil jwtUtil, RegisterRequestConverter registerRequestConverter, EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter, EmployeesRepository employeesRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.registerRequestConverter = registerRequestConverter;
        this.employeeToEmployeeResponseConverter = employeeToEmployeeResponseConverter;
        this.employeesRepository = employeesRepository;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        log.info("Initiating token generation for user: {}", loginRequest.userEmail());
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.userEmail(), loginRequest.password()
        );

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        log.info("Authentication successful for user: {}", loginRequest.userEmail());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwtToken = jwtUtil.generateToken((User) authentication.getPrincipal());
        log.info("JWT token generated successfully for user: {}",loginRequest.userEmail());

        return ResponseEntity.ok(new LoginResponse(jwtToken));
    }

    @Override
    public ResponseEntity<EmployeeResponse> registerEmployee(RegisterRequest registerRequest) {
        log.info("Attempting to create a new employee with email: {}", registerRequest.email());
        checkWhetherEmployeeAlreadyExist(registerRequest.email());

        Employees employee = registerRequestConverter.convert(registerRequest);

        Employees savedEmployee = employeesRepository.save(employee);

        EmployeeResponse employeeResponse = employeeToEmployeeResponseConverter.convert(savedEmployee);

        return ResponseEntity.status(HttpStatus.CREATED).body(employeeResponse);
    }

    private void checkWhetherEmployeeAlreadyExist(String email) {
        Boolean doesEmployeeExist = employeesRepository.existsByEmail(email);

        if (doesEmployeeExist) {
            log.error("User with email {} already exists", email);
            throw new UserAlreadyExistsException();
        }
    }
}
