package za.gov.helpdesk.users.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.users.dto.request.AdminPasswordResetRequest;
import za.gov.helpdesk.users.dto.response.UserResponse;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.security.CustomUserDetails;
import za.gov.helpdesk.users.service.AccountAdminService;
import za.gov.helpdesk.users.service.PasswordManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller restricted exclusively to system operators managing account statuses, lifecycle
 * deactivations, and administrative security resets.
 */
@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@Tag(
        name = "User Administration",
        description =
                "Privileged administrative operations for managing user accounts, "
                        + "access roles, and credential overrides.")
public class AdminController {

    private final AccountAdminService accountAdminService;
    private final PasswordManagementService passwordManagementService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a user account (Admin only)")
    public void deactivateUser(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {
        accountAdminService.deactivateUser(id, principal.getUser());
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Reactivate a locked user account (Admin only)",
            description = "Unlocks the account and " + "resets failed login attempts.")
    public void reactivateUser(
            @PathVariable final Long id,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        accountAdminService.reactivateUser(id, principal.getUser());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Change a user's role (Admin only)",
            description =
                    "Changes role and revokes all active "
                            + "sessions. Admin cannot change their own role.")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable final Long id,
            @RequestParam final User.Role role,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        return ResponseEntity.ok(accountAdminService.changeUserRole(id, role, principal.getUser()));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Admin resets a user's password (Admin only)",
            description =
                    "Resets password without requiring the current password. Revokes all active"
                            + " sessions.")
    public void adminResetPassword(
            @PathVariable final Long id,
            @Valid @RequestBody final AdminPasswordResetRequest request,
            @AuthenticationPrincipal final CustomUserDetails principal) {

        passwordManagementService.adminResetPassword(id, request, principal.getUser());
    }
}
