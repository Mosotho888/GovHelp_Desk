package com.loggingsystem.springjwtauth.employee.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeProfileResponseDTO;
import com.loggingsystem.springjwtauth.employee.exception.UserNotFoundException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.SubmittedTicketsDTO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EmployeesServices {

    private final EmployeesRepository employeesRepository;
    private final TicketUtils ticketUtils;

    public EmployeesServices(EmployeesRepository employeesRepository, TicketUtils ticketUtils) {
        this.employeesRepository = employeesRepository;
        this.ticketUtils = ticketUtils;
    }

    /**
     * Retrieves all employees with pagination and sorting.
     * Only accessible by users with the ADMIN role.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeProfileResponseDTO>> getAllEmployees(Pageable pageable) {
        log.info("Fetching all employees with pagination: page number {}, page size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeProfileResponseDTO> employeeProfileResponseDTOS = mapToEmployeeDTO(page);


        log.info("Found {} employees", employeeProfileResponseDTOS.size());

        return ResponseEntity.ok(employeeProfileResponseDTOS);
    }

    /**
     * Retrieves an employee by their ID.
     * Only accessible by users with the ADMIN role.
     *
     * @param id the ID of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeProfileResponseDTO> getEmployeeById(Long id) {
        log.info("Fetching employee by ID: {}", id);
        Employees employeeDetails = getEmployee(id);
        EmployeeProfileResponseDTO employeeResponseDetails = new EmployeeProfileResponseDTO(employeeDetails);



        return ResponseEntity.ok(employeeResponseDetails);
    }

    /**
     * Retrieves an employee's profile using their email.
     * Accessible by users with either ADMIN or USER roles.
     *
     * @param email the email of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<EmployeeProfileResponseDTO> getEmployeeProfileByEmail(String email) {
        log.info("Fetching employee by email: {}", email);

        Employees employee = getEmployeeByEmail(email);
        EmployeeProfileResponseDTO employeeResponseProfile = new EmployeeProfileResponseDTO(employee);

        setTicketsBasedOnRole(email, employee, employeeResponseProfile);

        return ResponseEntity.ok(employeeResponseProfile);
    }

    private void setTicketsBasedOnRole(String email, Employees employee, EmployeeProfileResponseDTO employeeResponseProfile) {
        if (isAdmin(employee)) {
            // Fetch and set assigned tasks if the user is an admin or technician
            List<AssignedTicketsDTO> tickets = ticketUtils.getAssignedTickets(employee);
            employeeResponseProfile.setAssignedTicketsBasedOnRole(employee.getRole(), tickets);
        } else {
            // Normal users only see their submitted tickets
            List<SubmittedTicketsDTO> tickets = ticketUtils.getTicketsByOwner(email);
            employeeResponseProfile.setSubmittedTicketsBasedOnRole(employee.getRole(), tickets);
        }
    }

    private static boolean isAdmin(Employees employee) {
        return employee.getRole().equals("ADMIN");
    }

    /**
     * Retrieves a list of technicians (employees with role ADMIN) with pagination and sorting.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    public ResponseEntity<List<EmployeeProfileResponseDTO>> getAllTechnicians(Pageable pageable) {
        String role = "ADMIN";
        log.info("Fetching all technicians with role: {} and pagination: page number {}, page size {}",
                role, pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeProfileResponseDTO> employeeProfileResponseDTOS = mapToEmployeeDTO(page);

        log.info("Found {} technicians", employeeProfileResponseDTOS.size());

        return ResponseEntity.ok(employeeProfileResponseDTOS);
    }

    /**
     * Maps a page of Employees to a list of EmployeeResponseDTO objects.
     *
     * @param page the page of Employees to map.
     * @return a list of EmployeeResponseDTO objects.
     */
    @NotNull
    private List<EmployeeProfileResponseDTO> mapToEmployeeDTO(Page<Employees> page) {
        return page.getContent()
                .stream()
                .map(employee -> {
                    EmployeeProfileResponseDTO dto = new EmployeeProfileResponseDTO(employee);
                    setTicketsBasedOnRole(employee.getEmail(), employee, dto);
                    return dto;
                })
                .toList();
    }

    /**
     * Retrieves an employee by their ID.
     *
     * @param id the ID of the employee.
     * @return the Employee entity.
     * @throws UserNotFoundException if the employee is not found.
     */
    public Employees getEmployee(Long id) {
        Optional<Employees> optionalEmployees = employeesRepository.findById(id);

        if (optionalEmployees.isPresent()) {
            log.info("Employee found with ID: {}", id);
            return  optionalEmployees.get();
        }

        log.warn("No employee found with ID: {}", id);
        throw new UserNotFoundException();
    }

    /**
     * Retrieves an employee by their email.
     *
     * @param email the email of the employee.
     * @return the Employee entity.
     * @throws UserNotFoundException if the employee is not found.
     */
    @NotNull
    public Employees getEmployeeByEmail(String email) {
        Optional<Employees> employeeProfile = employeesRepository.findByEmail(email);

        if (employeeProfile.isPresent()) {
            log.info("Employee found with email: {}", email);

            return employeeProfile.get();
        }

        log.warn("No employee found with email: {}", email);
        throw new UserNotFoundException();
    }


    public ResponseEntity<Void> deleteEmployeeById(Long employeeId) {
        Employees employees = getEmployee(employeeId);

        employeesRepository.deleteById(employees.getId());

        return ResponseEntity.noContent().build();
    }
}
