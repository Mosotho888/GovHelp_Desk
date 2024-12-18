package com.loggingsystem.springjwtauth.exception;

public class TicketNotFoundException extends CustomException{
    public TicketNotFoundException() {
        super(ErrorMessages.TICKET_NOT_FOUND);
    }
}
