package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import com.loggingsystem.springjwtauth.service.EmployeesServices;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeesController {
    private final EmployeesServices employeesServices;

    public EmployeesController(EmployeesServices employeesServices) {
        this.employeesServices = employeesServices;
    }

    @GetMapping
    private ResponseEntity<List<EmployeeResponseDTO>> findAllEmployees (Pageable pageable) {
        return employeesServices.findAll(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<Employees> findEmployeeById (@PathVariable Long id) {
        return employeesServices.findById(id);
    }

    @GetMapping("/profile")
    private ResponseEntity<Employees> findEmployeeByEmail(Principal principal) {
        return employeesServices.findByEmail(principal);
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<EmployeeResponseDTO>> findAllTechnicians(Pageable pageable) {
        return employeesServices.findAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
