package za.gov.helpdesk.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import za.gov.helpdesk.users.dto.request.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.gov.helpdesk.users.dto.request.UpdateUserRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.service.UserService;

@RestController
@RequestMapping("/v1/users")
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List of all users (Admin only)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 25, sort = "createdAt")  Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a user account (Admin only)")
    public void deactivateUser (@PathVariable Long id) {
        userService.deactivateUser(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Update user profile")
    private ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
}
