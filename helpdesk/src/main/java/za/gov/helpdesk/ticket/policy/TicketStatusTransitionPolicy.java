package za.gov.helpdesk.ticket.policy;

import org.springframework.stereotype.Component;
import za.gov.helpdesk.ticket.exception.InvalidStatusTransitionException;
import za.gov.helpdesk.ticket.model.Ticket;

@Component
public class TicketStatusTransitionPolicy {

    public boolean canTransition(Ticket.Status current, Ticket.Status next) {
        return switch (current) {
            case OPEN, ESCALATED -> next == Ticket.Status.IN_PROGRESS;
            case IN_PROGRESS -> next == Ticket.Status.RESOLVED || next == Ticket.Status.ESCALATED;
            case RESOLVED -> next == Ticket.Status.CLOSED || next == Ticket.Status.OPEN;
            case CLOSED -> false;
        };
    }

    public void assertCanTransition(Ticket.Status current, Ticket.Status next) {
        if (!canTransition(current, next)) {
            throw new InvalidStatusTransitionException(current, next);
        }
    }
}
