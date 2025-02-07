package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import com.loggingsystem.springjwtauth.ticketcomment.repository.TicketCommentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GetAllCommentsByTicketIdService {
    private final TicketCommentsRepository ticketCommentsRepository;

    public GetAllCommentsByTicketIdService(TicketCommentsRepository ticketCommentsRepository) {
        this.ticketCommentsRepository = ticketCommentsRepository;
    }

    public ResponseEntity<List<CommentResponseDTO>> getAllCommentsByTicketId(Long id) {
        log.info("Fetching all comments for ticket ID: {}", id);
        List<TicketComments> ticketComments = ticketCommentsRepository.findAllByTickets_id(id);
        List<CommentResponseDTO> commentResponseDTOs = ticketComments.stream().map(CommentResponseDTO::new).toList();

        log.info("Fetched {} comments for ticket ID: {}", commentResponseDTOs.size(), id);
        return ResponseEntity.ok(commentResponseDTOs);
    }
}
