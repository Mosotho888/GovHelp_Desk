package com.loggingsystem.springjwtauth.employee.controller;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeProfileResponse;
import com.loggingsystem.springjwtauth.employee.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/employees")
@Slf4j
public class EmployeesController {
    private final EmployeeService employeeService;

    public EmployeesController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    private ResponseEntity<List<EmployeeProfileResponse>> findAllEmployees (Pageable pageable) {
        return employeeService.getAllEmployees(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<EmployeeProfileResponse> findEmployeeById (@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployeeById (@PathVariable Long employeeId) {
        return employeeService.deleteEmployeeById(employeeId);
    }

    @GetMapping("/profile")
    private ResponseEntity<EmployeeProfileResponse> findEmployeeByEmail(Principal principal) {
        return employeeService.getEmployeeProfileByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<EmployeeProfileResponse>> findAllTechnicians(Pageable pageable) {
        return employeeService.getAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
