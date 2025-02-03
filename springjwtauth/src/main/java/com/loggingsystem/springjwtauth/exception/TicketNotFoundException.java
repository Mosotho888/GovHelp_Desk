package com.loggingsystem.springjwtauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException() {
        super(ErrorMessages.TICKET_NOT_FOUND.getMessage());
    }
}
