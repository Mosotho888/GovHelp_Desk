package com.loggingsystem.springjwtauth.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException() {
        super(ErrorMessages.TICKET_NOT_FOUND.getMessage());
    }
}
