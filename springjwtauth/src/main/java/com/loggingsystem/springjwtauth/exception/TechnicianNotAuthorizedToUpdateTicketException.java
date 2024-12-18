package com.loggingsystem.springjwtauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TechnicianNotAuthorizedToUpdateTicketException extends CustomException{
    private String code;
    private HttpStatus status;
    public TechnicianNotAuthorizedToUpdateTicketException() {
        super(ErrorMessages.TECHNICIAN_NOT_AUTHORIZED_TO_UPDATE_TICKET);
    }
}
