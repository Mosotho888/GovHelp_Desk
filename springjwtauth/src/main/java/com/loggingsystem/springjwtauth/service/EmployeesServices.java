package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
                LocalDateTime.now(),
                null,
                "USER");


        Employees savedEmployee = employeesRepository.save(newEmployee);

        URI locationOfNewLocation = ucb.path("/api/auth/register/{id}")
                .buildAndExpand(savedEmployee.getId())
                .toUri();

        return ResponseEntity.created(locationOfNewLocation).build();
    }

    public ResponseEntity<List<EmployeeResponseDTO>> findAll(Pageable pageable) {
        Page<Employees> page = employeesRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        // Map Employees to employeeResponseDTO
        List<EmployeeResponseDTO> employeeResponseDTOs = page.getContent()
                .stream()
                .map(EmployeeResponseDTO::new) // Assuming a constructor exists for mapping
                .toList();

        return ResponseEntity.ok(employeeResponseDTOs);
    }

    public ResponseEntity<Employees> findById(Long id) {
        Optional<Employees> employee = employeesRepository.findById(id);

        if (employee.isPresent()) {
            return ResponseEntity.ok(employee.get());
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Employees> findByEmail(Principal principal) {
        Optional<Employees> employeeProfile = employeesRepository.findByEmail(principal.getName());

        if (employeeProfile.isPresent()) {
            return ResponseEntity.ok(employeeProfile.get());
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<EmployeeResponseDTO>> findAllTechnicians(Pageable pageable) {
        String role = "ADMIN";

        Page<Employees> page = employeesRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeResponseDTO> employeeResponseDTOs = page.getContent()
                .stream()
                .map(EmployeeResponseDTO::new) // Assuming a constructor exists for mapping
                .toList();

        return ResponseEntity.ok(employeeResponseDTOs);
    }
}
