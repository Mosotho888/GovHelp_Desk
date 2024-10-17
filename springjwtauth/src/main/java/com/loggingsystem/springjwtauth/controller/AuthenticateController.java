package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.service.EmployeesServices;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthenticateController {
    private final EmployeesServices employeesServices;

    public AuthenticateController(EmployeesServices employeesServices) {
        this.employeesServices = employeesServices;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerEmployee(@Valid @RequestBody Employees newEmployeeRequest, UriComponentsBuilder ucb) {

        return employeesServices.createEmployee(newEmployeeRequest, ucb);
    }
}
