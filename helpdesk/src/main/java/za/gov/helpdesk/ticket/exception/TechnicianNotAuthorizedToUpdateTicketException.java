package za.gov.helpdesk.ticket.exception;

import za.gov.helpdesk.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TechnicianNotAuthorizedToUpdateTicketException extends RuntimeException {

    public TechnicianNotAuthorizedToUpdateTicketException() {
        super(ErrorMessages.TECHNICIAN_NOT_AUTHORIZED_TO_UPDATE_TICKET.getMessage());
    }
}
