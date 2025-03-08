package com.loggingsystem.springjwtauth.employee.service.impl;

import com.loggingsystem.springjwtauth.common.util.EmployeeUtil;
import com.loggingsystem.springjwtauth.common.util.TicketUtil;
import com.loggingsystem.springjwtauth.employee.dto.EmployeeProfileResponse;
import com.loggingsystem.springjwtauth.employee.exception.UserNotFoundException;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.repository.EmployeesRepository;
import com.loggingsystem.springjwtauth.employee.service.EmployeeService;
import com.loggingsystem.springjwtauth.ticket.dto.AssignedTicketsDTO;
import com.loggingsystem.springjwtauth.ticket.dto.SubmittedTicketsDTO;
import lombok.extern.slf4j.Slf4j;
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
public class EmployeesServicesImpl implements EmployeeService {

    private final EmployeesRepository employeesRepository;
    private final TicketUtil ticketUtil;
    private final EmployeeUtil employeeUtil;

    public EmployeesServicesImpl(EmployeesRepository employeesRepository, TicketUtil ticketUtil, EmployeeUtil employeeUtil) {
        this.employeesRepository = employeesRepository;
        this.ticketUtil = ticketUtil;
        this.employeeUtil = employeeUtil;
    }

    /**
     * Retrieves all employees with pagination and sorting.
     * Only accessible by users with the ADMIN role.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeProfileResponse>> getAllEmployees(Pageable pageable) {
        log.info("Fetching all employees with pagination: page number {}, page size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeProfileResponse> employeeProfileResponses = mapToEmployeeDTO(page);


        log.info("Found {} employees", employeeProfileResponses.size());

        return ResponseEntity.ok(employeeProfileResponses);
    }

    /**
     * Retrieves an employee by their ID.
     * Only accessible by users with the ADMIN role.
     *
     * @param employeeId the ID of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeProfileResponse> getEmployeeById(Long employeeId) {
        log.info("Fetching employee by ID: {}", employeeId);
        Employees employeeDetails = employeeUtil.getEmployee(employeeId);
        EmployeeProfileResponse employeeResponseDetails = new EmployeeProfileResponse(employeeDetails);

        return ResponseEntity.ok(employeeResponseDetails);
    }

    /**
     * Retrieves an employee's profile using their email.
     * Accessible by users with either ADMIN or USER roles.
     *
     * @param email the email of the employee to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<EmployeeProfileResponse> getEmployeeProfileByEmail(String email) {
        log.info("Fetching employee by email: {}", email);

        Employees employee = employeeUtil.getEmployeeByEmail(email);
        EmployeeProfileResponse employeeResponseProfile = new EmployeeProfileResponse(employee);

        setTicketsBasedOnRole(email, employee, employeeResponseProfile);

        return ResponseEntity.ok(employeeResponseProfile);
    }

    private void setTicketsBasedOnRole(String email, Employees employee, EmployeeProfileResponse employeeResponseProfile) {
        if (isAdmin(employee)) {
            // Fetch and set assigned tasks if the user is an admin or technician
            List<AssignedTicketsDTO> tickets = ticketUtil.getAssignedTickets(employee);
            employeeResponseProfile.setAssignedTicketsBasedOnRole(employee.getRole(), tickets);
        } else {
            // Normal users only see their submitted tickets
            List<SubmittedTicketsDTO> tickets = ticketUtil.getTicketsByOwner(email);
            employeeResponseProfile.setSubmittedTicketsBasedOnRole(employee.getRole(), tickets);
        }
    }

    private boolean isAdmin(Employees employee)
    {
        return employee.getRole().equals("ADMIN");
    }

    /**
     * Retrieves a list of technicians (employees with role ADMIN) with pagination and sorting.
     *
     * @param pageable pagination and sorting details.
     * @return ResponseEntity containing a list of EmployeeResponseDTO objects.
     */
    @Override
    public ResponseEntity<List<EmployeeProfileResponse>> getAllTechnicians(Pageable pageable) {
        String role = "ADMIN";
        log.info("Fetching all technicians with role: {} and pagination: page number {}, page size {}",
                role, pageable.getPageNumber(), pageable.getPageSize());

        Page<Employees> page = employeesRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<EmployeeProfileResponse> employeeProfileResponses = mapToEmployeeDTO(page);

        log.info("Found {} technicians", employeeProfileResponses.size());

        return ResponseEntity.ok(employeeProfileResponses);
    }

    /**
     * Maps a page of Employees to a list of EmployeeResponseDTO objects.
     *
     * @param page the page of Employees to map.
     * @return a list of EmployeeResponseDTO objects.
     */
    private List<EmployeeProfileResponse> mapToEmployeeDTO(Page<Employees> page) {
        return page.getContent()
                .stream()
                .map(employee -> {
                    EmployeeProfileResponse dto = new EmployeeProfileResponse(employee);
                    setTicketsBasedOnRole(employee.getEmail(), employee, dto);
                    return dto;
                })
                .toList();
    }

    @Override
    public ResponseEntity<Void> deleteEmployeeById(Long employeeId) {
        Employees employees = employeeUtil.getEmployee(employeeId);

        employeesRepository.deleteById(employees.getId());

        return ResponseEntity.noContent().build();
    }
}
