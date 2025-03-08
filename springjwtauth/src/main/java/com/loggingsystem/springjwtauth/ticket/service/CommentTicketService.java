package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

public interface CommentTicketService {
    ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal);
    ResponseEntity<List<CommentResponse>> getAllCommentsByTicketId(Long ticketId);
}
