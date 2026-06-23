package za.gov.helpdesk.sla.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import za.gov.helpdesk.exception.ResourceNotFoundException;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.sla.model.TicketSla;
import za.gov.helpdesk.sla.repository.SlaPolicyRepository;
import za.gov.helpdesk.sla.repository.TicketSlaRepository;
import za.gov.helpdesk.ticket.model.Ticket;

import lombok.RequiredArgsConstructor;

/**
 * Service utility component acting as a read-only query helper for SLA business rules and metadata
 * tracking. Provides abstracted database retrieval wrappers that encapsulate standard
 * entity-absence error tracking, running entirely inside a read-only transaction state optimization
 * boundary.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlaQueryHelper {

    private final TicketSlaRepository ticketSlaRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    /**
     * Resolves the runtime tracking metrics row associated with a given unique ticket identifier.
     * Throws a structured API resource exception if tracking metadata has not been initialized.
     *
     * @param ticketId the primary database identifier key of the targeted ticket aggregate root
     * @return the associated {@link TicketSla} tracker state container
     * @throws ResourceNotFoundException if no tracking row item is found matching the provided
     *     identity parameter
     */
    public TicketSla findByTicketOrThrow(final Long ticketId) {
        return ticketSlaRepository
                .findByTicketId(ticketId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("SLA metadata for ticket", ticketId));
    }

    /**
     * Resolves the system-wide threshold configuration policy assigned to a specific ticket
     * importance priority tier. Throws an illegal state runtime variance exception if an
     * administrator has neglected to configure baseline parameters.
     *
     * @param priority the active {@link Ticket.Priority} level classification to inspect
     * @return the corresponding {@link SlaPolicy} rule configuration definition
     * @throws IllegalStateException if the target classification rules are completely absent from
     *     the database
     */
    public SlaPolicy getPolicyOrThrow(final Ticket.Priority priority) {
        return slaPolicyRepository
                .findByPriority(priority)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No SLA policy configured for system priority: "
                                                + priority));
    }
}
