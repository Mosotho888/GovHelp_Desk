package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.model.JwtRequest;
import com.loggingsystem.springjwtauth.model.JwtResponse;
import com.loggingsystem.springjwtauth.service.EmployeesServices;
import com.loggingsystem.springjwtauth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticateController {
    private final EmployeesServices employeesServices;
    private final JwtService jwtService;

    public AuthenticateController(EmployeesServices employeesServices, JwtService jwtService) {
        this.employeesServices = employeesServices;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerEmployee(@Valid @RequestBody Employees newEmployeeRequest) {

        return employeesServices.createEmployee(newEmployeeRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login (@RequestBody JwtRequest jwtRequest) {
        return jwtService.generateToken(jwtRequest);
    }

    //@PostMapping("/logout")
    //PostMapping("/refresh")
}
