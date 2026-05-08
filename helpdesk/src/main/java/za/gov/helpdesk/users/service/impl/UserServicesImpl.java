package za.gov.helpdesk.users.service.impl;

import org.springframework.http.HttpStatus;
import za.gov.helpdesk.users.dto.RegisterRequest;
import za.gov.helpdesk.common.util.EmployeeUtil;
import za.gov.helpdesk.common.util.TicketUtil;
import za.gov.helpdesk.users.dto.UserProfileResponse;
import za.gov.helpdesk.users.dto.UserResponse;
import za.gov.helpdesk.users.exception.UserAlreadyExistsException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.service.EmployeeService;
import za.gov.helpdesk.ticket.dto.AssignedTicketsDTO;
import za.gov.helpdesk.ticket.dto.SubmittedTicketsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserServicesImpl implements EmployeeService {

    private final za.gov.helpdesk.users.repository.UserRepository userRepository;
    private final TicketUtil ticketUtil;
    private final EmployeeUtil employeeUtil;

    public UserServicesImpl(za.gov.helpdesk.users.repository.UserRepository userRepository, TicketUtil ticketUtil, EmployeeUtil employeeUtil) {
        this.userRepository = userRepository;
        this.ticketUtil = ticketUtil;
        this.employeeUtil = employeeUtil;
    }

//    @Override
//    public UserResponse registerEmployee(RegisterRequest registerRequest) {
//        log.info("Attempting to create a new users with email: {}", registerRequest.email());
//        checkWhetherEmployeeAlreadyExist(registerRequest.email());
//
//        za.gov.helpdesk.users.model.User employee = registerRequestConverter.convert(registerRequest);
//
//        za.gov.helpdesk.users.model.User savedEmployee = userRepository.save(employee);
//
//        za.gov.helpdesk.users.dto.UserResponse userResponse = employeeToEmployeeResponseConverter.convert(savedEmployee);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
//    }

    private void checkWhetherEmployeeAlreadyExist(String email) {
        Boolean doesEmployeeExist = userRepository.existsByEmail(email);

        if (doesEmployeeExist) {
            log.error("User with email {} already exists", email);
            throw new UserAlreadyExistsException();
        }
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
    public ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> getAllEmployees(Pageable pageable) {
        log.info("Fetching all employees with pagination: page number {}, page size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<za.gov.helpdesk.users.model.User> page = userRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<za.gov.helpdesk.users.dto.UserProfileResponse> userProfileRespons = mapToEmployeeDTO(page);


        log.info("Found {} employees", userProfileRespons.size());

        return ResponseEntity.ok(userProfileRespons);
    }

    /**
     * Retrieves an users by their ID.
     * Only accessible by users with the ADMIN role.
     *
     * @param employeeId the ID of the users to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> getEmployeeById(Long employeeId) {
        log.info("Fetching users by ID: {}", employeeId);
        za.gov.helpdesk.users.model.User employeeDetails = employeeUtil.getEmployee(employeeId);
        za.gov.helpdesk.users.dto.UserProfileResponse employeeResponseDetails = new za.gov.helpdesk.users.dto.UserProfileResponse(employeeDetails);

        return ResponseEntity.ok(employeeResponseDetails);
    }

    /**
     * Retrieves an users's profile using their email.
     * Accessible by users with either ADMIN or USER roles.
     *
     * @param email the email of the users to retrieve.
     * @return ResponseEntity containing the Employee entity.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<UserProfileResponse> getEmployeeProfileByEmail(String email) {
        log.info("Fetching users by email: {}", email);

        za.gov.helpdesk.users.model.User employee = employeeUtil.getEmployeeByEmail(email);
        za.gov.helpdesk.users.dto.UserProfileResponse employeeResponseProfile = new za.gov.helpdesk.users.dto.UserProfileResponse(employee);

        setTicketsBasedOnRole(email, employee, employeeResponseProfile);

        return ResponseEntity.ok(employeeResponseProfile);
    }

    private void setTicketsBasedOnRole(String email, User employee, UserProfileResponse employeeResponseProfile) {
        if (isAdmin(employee)) {
            // Fetch and set assigned tasks if the user is an admin or technician
            List<AssignedTicketsDTO> tickets = ticketUtil.getAssignedTickets(employee);
            employeeResponseProfile.setAssignedTicketsBasedOnRole(employee.getRole().toString(), tickets);
        } else {
            // Normal users only see their submitted tickets
            List<SubmittedTicketsDTO> tickets = ticketUtil.getTicketsByOwner(email);
            employeeResponseProfile.setSubmittedTicketsBasedOnRole(employee.getRole().toString(), tickets);
        }
    }

    private boolean isAdmin(User employee)
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
    public ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> getAllTechnicians(Pageable pageable) {
        String role = "ADMIN";
        log.info("Fetching all technicians with role: {} and pagination: page number {}, page size {}",
                role, pageable.getPageNumber(), pageable.getPageSize());

        Page<za.gov.helpdesk.users.model.User> page = userRepository.findAllByRole(role, PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        List<za.gov.helpdesk.users.dto.UserProfileResponse> userProfileRespons = mapToEmployeeDTO(page);

        log.info("Found {} technicians", userProfileRespons.size());

        return ResponseEntity.ok(userProfileRespons);
    }

    /**
     * Maps a page of User to a list of EmployeeResponseDTO objects.
     *
     * @param page the page of User to map.
     * @return a list of EmployeeResponseDTO objects.
     */
    private List<za.gov.helpdesk.users.dto.UserProfileResponse> mapToEmployeeDTO(Page<za.gov.helpdesk.users.model.User> page) {
        return page.getContent()
                .stream()
                .map(employee -> {
                    za.gov.helpdesk.users.dto.UserProfileResponse dto = new za.gov.helpdesk.users.dto.UserProfileResponse(employee);
                    setTicketsBasedOnRole(employee.getEmail(), employee, dto);
                    return dto;
                })
                .toList();
    }

    @Override
    public ResponseEntity<Void> deleteEmployeeById(Long employeeId) {
        za.gov.helpdesk.users.model.User user = employeeUtil.getEmployee(employeeId);

        userRepository.deleteById(user.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public UserResponse registerEmployee(RegisterRequest registerRequest) {
        return null;
    }
}
