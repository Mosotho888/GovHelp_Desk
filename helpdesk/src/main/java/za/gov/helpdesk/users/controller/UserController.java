package za.gov.helpdesk.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import za.gov.helpdesk.users.dto.CreateUserRequest;
import za.gov.helpdesk.users.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.gov.helpdesk.users.dto.UserResponse;
import za.gov.helpdesk.users.service.UserService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class UserController {
    private final UserService userService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user account (Admin only)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    private ResponseEntity<UserResponse> getUserById (@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployeeById (@PathVariable Long employeeId) {
        return UserService.deleteEmployeeById(employeeId);
    }

    @GetMapping("/profile")
    private ResponseEntity<za.gov.helpdesk.users.dto.UserProfileResponse> findEmployeeByEmail(Principal principal) {
        return UserService.getEmployeeProfileByEmail(principal.getName());
    }

    @GetMapping("/technicians")
    private ResponseEntity<List<za.gov.helpdesk.users.dto.UserProfileResponse>> findAllTechnicians(Pageable pageable) {
        return UserService.getAllTechnicians(pageable);
    }
//    @PutMapping("/profile")
//    @PostMapping("/{id}/roles")
}
