package za.gov.helpdesk.auth.controller;

import za.gov.helpdesk.auth.dto.LoginResponse;
import za.gov.helpdesk.auth.dto.RegisterRequest;
import za.gov.helpdesk.auth.service.AuthenticationService;
import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody RegisterRequest registerRequest) {

        return authenticationService.registerEmployee(registerRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
    }

    //@PostMapping("/logout")
    //PostMapping("/refresh")
}
