package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class EmployeesServices {

    private final EmployeesRepository employeesRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeesServices(EmployeesRepository employeesRepository, PasswordEncoder passwordEncoder) {
        this.employeesRepository = employeesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<Void> createEmployee(Employees newEmployeeRequest, UriComponentsBuilder ucb) {
        String encodedPassword = passwordEncoder.encode(newEmployeeRequest.getPassword());
        Employees newEmployee = new Employees(null,
                encodedPassword,
                newEmployeeRequest.getFirst_name(),
                newEmployeeRequest.getLast_name(),
                newEmployeeRequest.getEmail(),
                newEmployeeRequest.getPhone_number(),
                "USER");

        Employees savedEmployee = employeesRepository.save(newEmployee);

        URI locationOfNewLocation = ucb.path("/api/auth/register/{id}")
                .buildAndExpand(savedEmployee.getId())
                .toUri();

        return ResponseEntity.created(locationOfNewLocation).build();
    }
}
