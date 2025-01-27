package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.dto.EmployeeResponseDTO;
import com.loggingsystem.springjwtauth.exception.UserAlreadyExistsException;
import com.loggingsystem.springjwtauth.exception.UsernameNotFoundException;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.repository.EmployeesRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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

    /**
     * Creates a new employee. This method checks if the employee already exists by email,
     * encodes the password, and saves the employee to the repository.
     *
     * @param newEmployee the employee data to be used to create a new employee.
     * @return ResponseEntity with HTTP status CREATED.
     */
    public ResponseEntity<Void> createEmployee(Employees newEmployee) {
        log.info("Attempting to create a new employee with email: {}", newEmployee.getEmail());
        checkWhetherEmployeeAlreadyExist(newEmployee);

        String encodedPassword = passwordEncoder.encode(newEmployee.getPassword());
        log.info("Password encoded successfully for email: {}", newEmployee.getEmail());

        newEmployee.setPassword(encodedPassword);
        newEmployee.setCreated_at(LocalDateTime.now());
        newEmployee.setRole("USER");

        Employees savedEmployee = employeesRepository.save(newEmployee);
        log.info("Employee with email {} saved successfully", savedEmployee.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves all employees with pagination and sorting.
     * Only accessible by users with the ADMIN role.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees(Pageable pageable) {
        log.info("Fetching all employees with pagination: page number {}, page size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeResponseDTO> employeeResponseDTOs = mapToEmployeeDTO(page);

        log.info("Found {} employees", employeeResponseDTOs.size());

        return ResponseEntity.ok(employeeResponseDTOs);
    }

    /**
     * Retrieves an employee by their ID.
     * Only accessible by users with the ADMIN role.
     *
     * @param id the ID of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employees> getEmployeeById(Long id) {
        log.info("Fetching employee by ID: {}", id);
        Employees employeeDetails = getEmployee(id);

        return ResponseEntity.ok(employeeDetails);
    }

    /**
     * Retrieves an employee's profile using their email.
     * Accessible by users with either ADMIN or USER roles.
     *
     * @param email the email of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Employees> getEmployeeProfileByEmail(String email) {
        log.info("Fetching employee by email: {}", email);

        Employees employeeProfile = getEmployeeByEmail(email);

        return ResponseEntity.ok(employeeProfile);
    }

    /**
     * Retrieves a list of technicians (employees with role ADMIN) with pagination and sorting.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    public ResponseEntity<List<EmployeeResponseDTO>> getAllTechnicians(Pageable pageable) {
        String role = "ADMIN";
        log.info("Fetching all technicians with role: {} and pagination: page number {}, page size {}",
                role, pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeResponseDTO> employeeResponseDTOs = mapToEmployeeDTO(page);

        log.info("Found {} technicians", employeeResponseDTOs.size());

        return ResponseEntity.ok(employeeResponseDTOs);
    }

    /**
     * Checks if an employee with the same email already exists in the system.
     *
     * @param newEmployee the request object containing employee details.
     * @throws UserAlreadyExistsException if the employee with the same email already exists.
     */
    private void checkWhetherEmployeeAlreadyExist(Employees newEmployee) {
        Boolean doesEmployeeExist = employeesRepository.existsByEmail(newEmployee.getEmail());

        if (doesEmployeeExist) {
            log.error("User with email {} already exists", newEmployee.getEmail());
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Maps a page of Employees to a list of EmployeeResponseDTO objects.
     *
     * @param page the page of Employees to map.
     * @return a list of EmployeeResponseDTO objects.
     */
    @NotNull
    private static List<EmployeeResponseDTO> mapToEmployeeDTO(Page<Employees> page) {
        return page.getContent()
                .stream()
                .map(EmployeeResponseDTO::new)
                .toList();
    }

    /**
     * Retrieves an employee by their ID.
     *
     * @param id the ID of the employee.
     * @return the Employee entity.
     * @throws UsernameNotFoundException if the employee is not found.
     */
    private Employees getEmployee(Long id) {
        Optional<Employees> optionalEmployees = employeesRepository.findById(id);

        if (optionalEmployees.isPresent()) {
            log.info("Employee found with ID: {}", id);
            return  optionalEmployees.get();
        }

        log.warn("No employee found with ID: {}", id);
        throw new UsernameNotFoundException();
    }

    /**
     * Retrieves an employee by their email.
     *
     * @param email the email of the employee.
     * @return the Employee entity.
     * @throws UsernameNotFoundException if the employee is not found.
     */
    @NotNull
    private Employees getEmployeeByEmail(String email) {
        Optional<Employees> employeeProfile = employeesRepository.findByEmail(email);

        if (employeeProfile.isPresent()) {
            log.info("Employee found with email: {}", email);

            return employeeProfile.get();
        }

        log.warn("No employee found with email: {}", email);
        throw new UsernameNotFoundException();
    }
}
