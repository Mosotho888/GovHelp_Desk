package za.gov.helpdesk.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import za.gov.helpdesk.auth.dto.AuthResponse;
import za.gov.helpdesk.auth.dto.RegisterRequest;
import za.gov.helpdesk.auth.service.AuthService;
import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.auth.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and token management")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<AuthResponse> login (@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
        //return authService.login(loginRequest);
    }

    //PostMapping("/refresh")
}
