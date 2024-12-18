package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.exception.UserAlreadyExistsException;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Slf4j
public class EmployeesServices {

    private final EmployeesRepository employeesRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeesServices(EmployeesRepository employeesRepository, PasswordEncoder passwordEncoder) {
        this.employeesRepository = employeesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<Void> createEmployee(Employees newEmployeeRequest, UriComponentsBuilder ucb) {
        log.info("Attempting to create a new employee with email: {}", newEmployeeRequest.getEmail());

        Optional<Employees> optionalEmployee = employeesRepository.findByEmail(newEmployeeRequest.getEmail());

        if (optionalEmployee.isPresent()) {
            log.error("User with email {} already exists", newEmployeeRequest.getEmail());
            throw new UserAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(newEmployeeRequest.getPassword());
        log.info("Password encoded successfully for email: {}", newEmployeeRequest.getEmail());

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
        log.info("Employee with email {} saved successfully", savedEmployee.getEmail());

        URI locationOfNewLocation = ucb.path("/api/auth/register/{id}")
                .buildAndExpand(savedEmployee.getId())
                .toUri();

        return ResponseEntity.created(locationOfNewLocation).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponseDTO>> findAllEmployees(Pageable pageable) {
        log.info("Fetching all employees with pagination: page number {}, page size {}",
                pageable.getPageNumber(), pageable.getPageSize());

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

        log.info("Found {} employees", employeeResponseDTOs.size());

        return ResponseEntity.ok(employeeResponseDTOs);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employees> findEmployeeById(Long id) {
        log.info("Fetching employee by ID: {}", id);

        Optional<Employees> employee = employeesRepository.findById(id);

        if (employee.isPresent()) {
            log.info("Employee found with ID: {}", id);
            return ResponseEntity.ok(employee.get());
        }

        log.warn("No employee found with ID: {}", id);
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Employees> findEmployeeByEmail(String email) {
        log.info("Fetching employee by email: {}", email);

        Optional<Employees> employeeProfile = employeesRepository.findByEmail(email);

        if (employeeProfile.isPresent()) {
            log.info("Employee found with email: {}", email);
            return ResponseEntity.ok(employeeProfile.get());
        }

        log.warn("No employee found with email: {}", email);
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<EmployeeResponseDTO>> findAllTechnicians(Pageable pageable) {
        String role = "ROLE_ADMIN";
        log.info("Fetching all technicians with role: {} and pagination: page number {}, page size {}",
                role, pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeResponseDTO> employeeResponseDTOs = page.getContent()
                .stream()
                .map(EmployeeResponseDTO::new) // Assuming a constructor exists for mapping
                .toList();

        log.info("Found {} technicians", employeeResponseDTOs.size());

        return ResponseEntity.ok(employeeResponseDTOs);
    }
}
