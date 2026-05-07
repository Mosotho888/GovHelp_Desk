package za.gov.helpdesk.auth.service.impl;

import za.gov.helpdesk.auth.dto.LoginResponse;
import za.gov.helpdesk.auth.dto.RegisterRequest;
import za.gov.helpdesk.auth.jwt.JwtUtil;
import za.gov.helpdesk.auth.dto.LoginRequest;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.auth.service.RegisterRequestConverter;
import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.employee.exception.UserAlreadyExistsException;
import za.gov.helpdesk.employee.model.Employees;
import za.gov.helpdesk.employee.repository.EmployeesRepository;
import za.gov.helpdesk.employee.service.EmployeeToEmployeeResponseConverter;
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
public class AuthServiceImpl implements AuthService {
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

        String jwtToken = jwtUtil.generateAccessToken((User) authentication.getPrincipal());
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
