package za.gov.helpdesk.agent.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.agent.repository.jpa.AgentRepository;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Utility component providing helper methods for querying Agent records and validating access
 * rights.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentQueryHelper {

    private final AgentRepository agentRepository;

    /**
     * Retrieves an Agent by their unique identifier or throws a resource not found exception.
     *
     * @param id the unique identifier of the agent
     * @return the located {@link Agent} entity
     * @throws ResourceNotFoundException if no agent matches the provided identifier
     */
    public Agent findOrThrow(final Long id) {
        return agentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
    }

    /**
     * Retrieves an Agent and verifies if the requesting user has permission to access it.
     * Administrators have global access, while standard users can only access their own profile.
     *
     * @param id the unique identifier of the target agent
     * @param actor the authenticated {@link User} performing the operation
     * @return the validated {@link Agent} entity
     * @throws ResourceNotFoundException if the agent does not exist
     * @throws AccessDeniedException if the user lacks adequate permission boundaries
     */
    public Agent findAndValidateAccess(final Long id, final User actor) {
        final Agent agent = findOrThrow(id);

        // Secure target access boundaries: 403 if they exist but you aren't allowed to touch them
        if (actor.getRole() != User.Role.ADMIN && !agent.getUser().getId().equals(actor.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this agent profile");
        }

        return agent;
    }
}
