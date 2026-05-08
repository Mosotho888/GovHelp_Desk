package za.gov.helpdesk.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import za.gov.helpdesk.users.dto.RegisterRequest;
import za.gov.helpdesk.users.service.EmployeeService;
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
public class UserController {
    private final EmployeeService employeeService;

    public UserController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user")
    public ResponseEntity<za.gov.helpdesk.users.dto.UserResponse> registerEmployee(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.registerEmployee(registerRequest));
        //return authService.registerEmployee(registerRequest);
    }

    @GetMapping
    private ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> findAllEmployees (Pageable pageable) {
        return employeeService.getAllEmployees(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> findEmployeeById (@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployeeById (@PathVariable Long employeeId) {
        return employeeService.deleteEmployeeById(employeeId);
    }

    @GetMapping("/profile")
    private ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> findEmployeeByEmail(Principal principal) {
        return employeeService.getEmployeeProfileByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> findAllTechnicians(Pageable pageable) {
        return employeeService.getAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
