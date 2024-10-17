package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class EmployeesServices {

    private final EmployeesRepository employeesRepository;

    public EmployeesServices(EmployeesRepository employeesRepository) {
        this.employeesRepository = employeesRepository;
    }

    public ResponseEntity<Void> createEmployee(Employees newEmployeeRequest, UriComponentsBuilder ucb) {
        Employees newEmployee = new Employees(null,
                newEmployeeRequest.getPassword(),
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
