package za.gov.helpdesk.users.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.model.User;
import za.gov.helpdesk.users.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read-only database query utility component responsible for handling user profile retrieval
 * operations. Provides abstracted lookups by unique primary identifiers or electronic mail handles,
 * automatically enforcing uniform data normalization and throwing descriptive exceptions when
 * entities are missing. Optimization flags isolate all database connections within read-only
 * transactions.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryHelper {

    private final UserRepository userRepository;

    /**
     * Resolves a target user profile record by its primary relational database long identifier key.
     * Throws a structured exception mapping if the requested record cannot be located.
     *
     * @param userId the primary unique database identifier tracking the user record
     * @return the verified {@link User} entity model instance snapshot
     * @throws ResourceNotFoundException if no user entity matches the provided identity argument
     */
    public User findOrThrow(final Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Resolves a target user profile record by its electronic mail string handle. Automatically
     * trims leading or trailing whitespace buffers and down-cases characters to match normalized
     * persistence storage strategies before executing the query.
     *
     * @param email the raw, un-sanitized communication destination email string to search for
     * @return the verified {@link User} entity matching the normalized parameters
     * @throws ResourceNotFoundException if no tracking user entry is registered under the provided
     *     address handle
     */
    public User findByEmailOrThrow(final String email) {

        final String normalisedEmail = email != null ? email.toLowerCase().trim() : "";

        return userRepository
                .findByEmail(normalisedEmail)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "User not found with email: " + normalisedEmail));
    }

    /**
     * Resolves a user profile by identity and verifies if they need to be promoted to an AGENT.
     * Bypasses read-only locks to record updated security role attributes.
     *
     * @param userId the primary database query key
     * @return the updated user profile record
     */
    @Transactional
    public User findAndPromoteToAgentOrThrow(final Long userId) {
        final User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Promotes USER role to AGENT while safely leaving ADMIN permissions intact
        if (user.getRole() == User.Role.USER) {
            user.setRole(User.Role.AGENT);
            return userRepository.save(user);
        }
        return user;
    }
}
