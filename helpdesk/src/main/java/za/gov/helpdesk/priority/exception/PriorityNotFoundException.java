package za.gov.helpdesk.priority.exception;

import za.gov.helpdesk.exception.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PriorityNotFoundException extends RuntimeException{
    public PriorityNotFoundException() {
        super(ErrorMessages.PRIORITY_NOT_FOUND.getMessage());
    }
}
