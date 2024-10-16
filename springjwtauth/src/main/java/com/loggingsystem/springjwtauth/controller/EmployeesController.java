package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import com.loggingsystem.springjwtauth.service.EmployeesServices;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeesController {
    private EmployeesServices employeesServices;

    public EmployeesController(EmployeesServices employeesServices) {
        this.employeesServices = employeesServices;
    }

    @GetMapping
    private ResponseEntity<List<Employees>> findAll () {
        return ResponseEntity.ok(employeesServices.findAll().getBody());
    }

}
