package za.gov.helpdesk.ticket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.jpa.TicketRepository;
import za.gov.helpdesk.users.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Read-only domain query utility component responsible for enforcing multi-tenant data boundaries.
 * Evaluates a requesting user security principal's organizational role (ADMIN, AGENT, or USER) to
 * securely filter or mask individual ticket records and paginated lookups, preventing horizontal
 * privilege escalation.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryHelper {

    private final TicketRepository ticketRepository;

    /**
     * Resolves a single ticket record by its primary key identifier, applying strict role-based
     * security access controls.
     *
     * <ul>
     *   <li><b>ADMIN:</b> Grants unrestricted global read access.
     *   <li><b>AGENT:</b> Restricts access to unassigned tickets or those explicitly assigned to
     *       them.
     *   <li><b>USER:</b> Restricts access strictly to tickets created by the active user.
     * </ul>
     *
     * <p>Throws an obfuscated resource exception if the ticket is missing or if the active security
     * context lacks access rights, hiding the entity's existence.
     *
     * @param ticketId the primary unique database tracking identifier key of the ticket entity
     * @param actor the security {@link User} principal initiating the lookup request
     * @return the verified, secured {@link Ticket} entity model context
     * @throws ResourceNotFoundException if the record does not exist or if the operator is
     *     unauthorized
     */
    public Ticket findOrThrow(final Long ticketId, final User actor) {
        final Ticket ticket =
                ticketRepository
                        .findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        return switch (actor.getRole()) {
            case ADMIN -> ticket;
            case AGENT -> {
                // Agents can only view unassigned tickets or tickets assigned directly to them
                if (ticket.getAssignee() == null
                        || ticket.getAssignee().getUser().getEmail().equals(actor.getEmail())) {
                    yield ticket;
                }
                throw new ResourceNotFoundException(
                        "Ticket", ticketId); // Safe 404 to hide entity presence
            }
            case USER -> {
                // End users can only view tickets they created
                if (ticket.getRequester().getEmail().equals(actor.getEmail())) {
                    yield ticket;
                }
                throw new ResourceNotFoundException("Ticket", ticketId);
            }
        };
    }

    /**
     * Fetches a paginated, filtered record stream matching established search metrics and role
     * visibility boundaries. End-users are strictly bound to their historical requests, agents are
     * filtered to unassigned queues or personal workloads, and system administrators bypass
     * filtering rules to view global infrastructure datasets.
     *
     * @param status the optional lifecycle {@link Ticket.Status} filter criteria parameter, or null
     * @param priority the optional importance {@link Ticket.Priority} filter criteria parameter, or
     *     null
     * @param assigneeId the optional primary target identifier key of an assigned agent profile, or
     *     null
     * @param pageable pagination layout specifications including sorting variables and chunk
     *     constraints
     * @param actor the security {@link User} execution context requesting the dataset slice
     * @return a {@link Page} container holding the filtered collection of verified accessible
     *     ticket records
     */
    public Page<Ticket> findWithFiltersAndSecurity(
            final Ticket.Status status,
            final Ticket.Priority priority,
            final Long assigneeId,
            final Pageable pageable,
            final User actor) {
        // 1. Regular users only get their own tickets
        if (actor.getRole() == User.Role.USER) {
            return ticketRepository.findByRequester(actor, pageable);
        }

        // 2. Agents only get unassigned tickets or tickets assigned to them
        if (actor.getRole() == User.Role.AGENT) {
            final String statusStr = status != null ? status.name() : null;
            final String priorityStr = priority != null ? priority.name() : null;

            return ticketRepository.findWithFiltersForAgent(
                    statusStr, priorityStr, assigneeId, actor.getEmail(), pageable);
        }

        // 3. Admins get raw global system access
        return ticketRepository.findWithFilters(status, priority, assigneeId, pageable);
    }
}
