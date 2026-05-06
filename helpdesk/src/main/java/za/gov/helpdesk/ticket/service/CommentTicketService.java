package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import za.gov.helpdesk.ticketcomment.model.TicketComments;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

public interface CommentTicketService {
    ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal);
    ResponseEntity<List<CommentResponse>> getAllCommentsByTicketId(Long ticketId);
}
