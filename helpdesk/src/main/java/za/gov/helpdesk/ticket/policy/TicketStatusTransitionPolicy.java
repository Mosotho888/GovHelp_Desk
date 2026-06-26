package za.gov.helpdesk.ticket.policy;

import org.springframework.stereotype.Component;

import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.model.Ticket;

/**
 * Domain policy state machine component responsible for enforcing legal lifecycle status
 * transitions. Evaluates operational safety boundaries governing how a ticket progresses from its
 * structural opening through escalation, assignment, resolution, and final closure phases.
 */
@Component
public class TicketStatusTransitionPolicy {

    /**
     * Evaluates a state transition trajectory to verify if the requested progression matches
     * defined workflow business rules.
     *
     * @param current the active {@link Ticket.Status} context of the target ticket aggregate root
     * @param next the proposed destination {@link Ticket.Status} rule mutation target
     * @return true if the state transition pathway is legal and supported, false otherwise
     */
    public boolean canTransition(final Ticket.Status current, final Ticket.Status next) {
        return switch (current) {
            case OPEN, ESCALATED -> next == Ticket.Status.IN_PROGRESS;
            case IN_PROGRESS -> next == Ticket.Status.RESOLVED || next == Ticket.Status.ESCALATED;
            case RESOLVED -> next == Ticket.Status.CLOSED || next == Ticket.Status.OPEN;
            case CLOSED -> false;
        };
    }

    /**
     * Asserts that a state machine progression trajectory is valid. Throws an explicit, unmapped
     * domain constraint failure exception if the transition breaches established support workflow
     * policies.
     *
     * @param current the active {@link Ticket.Status} mapping of the underlying issue
     * @param next the proposed target {@link Ticket.Status} mutation checkpoint
     * @throws InvalidStatusTransitionException if the transition path configuration is completely
     *     unauthorized
     */
    public void assertCanTransition(final Ticket.Status current, final Ticket.Status next) {
        if (!canTransition(current, next)) {
            throw new InvalidStatusTransitionException(current, next);
        }
    }
}
