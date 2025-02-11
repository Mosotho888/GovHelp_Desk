package com.loggingsystem.springjwtauth.employee.controller;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeProfileResponseDTO;
import com.loggingsystem.springjwtauth.employee.service.EmployeesServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final EmployeesServices employeesServices;

    @Autowired
    public EmployeesController(EmployeesServices employeesServices) {
        this.employeesServices = employeesServices;
    }

    @GetMapping
    private ResponseEntity<List<EmployeeProfileResponseDTO>> findAllEmployees (Pageable pageable) {
        return employeesServices.getAllEmployees(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<EmployeeProfileResponseDTO> findEmployeeById (@PathVariable Long id) {
        return employeesServices.getEmployeeById(id);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployeeById (@PathVariable Long employeeId) {
        return employeesServices.deleteEmployeeById(employeeId);
    }

    @GetMapping("/profile")
    private ResponseEntity<EmployeeProfileResponseDTO> findEmployeeByEmail(Principal principal) {
        return employeesServices.getEmployeeProfileByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<EmployeeProfileResponseDTO>> findAllTechnicians(Pageable pageable) {
        return employeesServices.getAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
