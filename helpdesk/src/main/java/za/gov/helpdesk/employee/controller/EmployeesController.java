package za.gov.helpdesk.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import za.gov.helpdesk.employee.dto.RegisterRequest;
import za.gov.helpdesk.employee.dto.EmployeeProfileResponse;
import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.employee.service.EmployeeService;
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
public class EmployeesController {
    private final EmployeeService employeeService;

    public EmployeesController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user")
    public ResponseEntity<EmployeeResponse> registerEmployee(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.registerEmployee(registerRequest));
        //return authService.registerEmployee(registerRequest);
    }

    @GetMapping
    private ResponseEntity<List<EmployeeProfileResponse>> findAllEmployees (Pageable pageable) {
        return employeeService.getAllEmployees(pageable);
    }

    @GetMapping("/{id}")
    private ResponseEntity<EmployeeProfileResponse> findEmployeeById (@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployeeById (@PathVariable Long employeeId) {
        return employeeService.deleteEmployeeById(employeeId);
    }

    @GetMapping("/profile")
    private ResponseEntity<EmployeeProfileResponse> findEmployeeByEmail(Principal principal) {
        return employeeService.getEmployeeProfileByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<EmployeeProfileResponse>> findAllTechnicians(Pageable pageable) {
        return employeeService.getAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
