package com.loggingsystem.springjwtauth.auth.controller;

import com.loggingsystem.springjwtauth.auth.service.AuthenticationService;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.auth.dto.SignUpRequest;
import com.loggingsystem.springjwtauth.auth.dto.LoginResponse;
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
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody Employees newEmployeeRequest) {

        return authenticationService.registerEmployee(newEmployeeRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody SignUpRequest signUpRequest) {
        return authenticationService.login(signUpRequest);
    }

    //@PostMapping("/logout")
    //PostMapping("/refresh")
}
