package za.gov.helpdesk.ticket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import za.gov.helpdesk.ticket.model.Status;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(final Status from, final Status to) {
        super(
                "Cannot transition ticket from "
                        + from
                        + " to "
                        + to
                        + ". Check the allowed lifecycle transitions.");
    }
}
