package za.gov.helpdesk.ticket.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.category.service.CategoryQueryHelper;
import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.ticket.model.Priority;
import za.gov.helpdesk.ticket.model.Status;
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
    private final CategoryQueryHelper categoryQuery;

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
     * @param status the optional lifecycle {@link Status} filter criteria parameter, or null
     * @param priority the optional importance {@link Priority} filter criteria parameter, or null
     * @param assigneeId the optional primary target identifier key of an assigned agent profile, or
     *     null
     * @param categoryId optional category id to filter by; when {@code includeDescendants} is true
     *     this also matches every subcategory beneath it
     * @param includeDescendants whether {@code categoryId} should be expanded to its full subtree
     * @param pageable pagination layout specifications including sorting variables and chunk
     *     constraints
     * @param actor the security {@link User} execution context requesting the dataset slice
     * @return a {@link Page} container holding the filtered collection of verified accessible
     *     ticket records
     */
    public Page<Ticket> findWithFiltersAndSecurity(
            final Status status,
            final Priority priority,
            final Long assigneeId,
            final Long categoryId,
            final boolean includeDescendants,
            final Pageable pageable,
            final User actor) {
        // 1. Regular users only get their own tickets
        if (actor.getRole() == User.Role.USER) {
            return ticketRepository.findByRequester(actor, pageable);
        }

        final Set<Long> categoryIds = resolveCategoryIds(categoryId, includeDescendants);

        // 2. Agents only get unassigned tickets or tickets assigned to them
        if (actor.getRole() == User.Role.AGENT) {
            final String statusStr = status != null ? status.name() : null;
            final String priorityStr = priority != null ? priority.name() : null;

            return ticketRepository.findWithFiltersForAgent(
                    statusStr, priorityStr, assigneeId, categoryIds, actor.getEmail(), pageable);
        }

        // 3. Admins get raw global system access
        return ticketRepository.findWithFilters(
                status, priority, assigneeId, categoryIds, pageable);
    }

    /**
     * Expands {@code categoryId} to include its descendants when requested, so filtering by a
     * parent category (e.g. "Hardware") also surfaces tickets filed under its subcategories.
     */
    private Set<Long> resolveCategoryIds(final Long categoryId, final boolean includeDescendants) {
        if (categoryId == null) {
            return null;
        }
        if (!includeDescendants) {
            // Still validates the id exists via findOrThrow inside resolveWithDescendants would be
            // overkill for a single id; a plain existence check keeps the 404 behaviour consistent.
            categoryQuery.findOrThrow(categoryId);
            return Set.of(categoryId);
        }
        return categoryQuery.resolveWithDescendants(categoryId);
    }
}
