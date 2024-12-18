package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.service.EmployeesServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Slf4j
public class EmployeesController {
    private final EmployeesServices employeesServices;

    @Autowired
    public EmployeesController(EmployeesServices employeesServices) {
        this.employeesServices = employeesServices;
    }

    @GetMapping
    private ResponseEntity<List<EmployeeResponseDTO>> findAllEmployees (Pageable pageable) {
        return employeesServices.findAllEmployees(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<Employees> findEmployeeById (@PathVariable Long id) {
        return employeesServices.findEmployeeById(id);
    }

    @GetMapping("/profile")
    private ResponseEntity<Employees> findEmployeeByEmail(Principal principal) {
        return employeesServices.findEmployeeByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<EmployeeResponseDTO>> findAllTechnicians(Pageable pageable) {
        return employeesServices.findAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
