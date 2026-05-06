package za.gov.helpdesk.exception.global;

import za.gov.helpdesk.category.exception.CategoryAlreadyExistException;
import za.gov.helpdesk.employee.exception.UserAlreadyExistsException;
import za.gov.helpdesk.employee.exception.UserNotFoundException;
import za.gov.helpdesk.exception.model.ErrorResponse;
import za.gov.helpdesk.priority.exception.PriorityNotFoundException;
import za.gov.helpdesk.ticket.exception.TechnicianNotAuthorizedToUpdateTicketException;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(TechnicianNotAuthorizedToUpdateTicketException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleTechnicianNotAuthorizedToUpdateTicketException(TechnicianNotAuthorizedToUpdateTicketException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTicketNotFoundException(TicketNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(PriorityNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlePriorityNotFoundException(PriorityNotFoundException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(CategoryAlreadyExistException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCategoryAlreadyException(CategoryAlreadyExistException exception, WebRequest request) {
        return createErrorResponse(exception, HttpStatus.CONFLICT, request);
    }

    private ErrorResponse createErrorResponse(Exception exception, HttpStatus status, WebRequest request) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
}
