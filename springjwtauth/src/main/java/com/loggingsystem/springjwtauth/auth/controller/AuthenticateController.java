package com.loggingsystem.springjwtauth.auth.controller;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.auth.dto.JwtRequest;
import com.loggingsystem.springjwtauth.auth.dto.JwtResponse;
import com.loggingsystem.springjwtauth.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticateController {
    private final JwtService jwtService;

    public AuthenticateController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerEmployee(@Valid @RequestBody Employees newEmployeeRequest) {

        return jwtService.registerEmployee(newEmployeeRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login (@Valid @RequestBody JwtRequest jwtRequest) {
        return jwtService.generateToken(jwtRequest);
    }

    //@PostMapping("/logout")
    //PostMapping("/refresh")
}
