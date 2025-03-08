package com.loggingsystem.springjwtauth.ticket.service.impl;

import com.loggingsystem.springjwtauth.common.util.EmployeeUtil;
import com.loggingsystem.springjwtauth.common.util.TicketUtil;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSenderService;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.service.CommentTicketService;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import com.loggingsystem.springjwtauth.ticketcomment.repository.TicketCommentsRepository;
import com.loggingsystem.springjwtauth.ticketcomment.service.TicketCommentsToCommentResponseConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CommentTicketServiceImpl implements CommentTicketService {

    private final EmployeeUtil employeeUtil;
    private final TicketCommentsRepository ticketCommentsRepository;
    private final TicketUtil ticketUtil;
    private final MessageSenderService messageSenderService;
    private final TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter;

    public CommentTicketServiceImpl(EmployeeUtil employeeUtil, TicketCommentsRepository ticketCommentsRepository, TicketUtil ticketUtil, MessageSenderService messageSenderService, TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter) {
        this.employeeUtil = employeeUtil;
        this.ticketCommentsRepository = ticketCommentsRepository;
        this.ticketUtil = ticketUtil;
        this.messageSenderService = messageSenderService;
        this.ticketCommentsToCommentResponseConverter = ticketCommentsToCommentResponseConverter;
    }

    // return comment not void
    @Override
    public ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal) {
        log.info("Adding comment to ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = ticketUtil.getTicket(ticketId);
        Employees employee = employeeUtil.getEmployeeByEmail(principal.getName());

        comments.setTickets(ticket);
        comments.setCommenter(employee);

        ticketCommentsRepository.save(comments);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, comments.getComment());
        messageSenderService.sendTicketCommentMessage(emailRequest);

        log.info("Email notification request sent for comment: {}", comments.getId());

        log.info("Comment added successfully to ticket ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<List<CommentResponse>> getAllCommentsByTicketId(Long ticketId) {
        log.info("Fetching all comments for ticket ID: {}", ticketId);
        List<TicketComments> ticketComments = ticketCommentsRepository.findAllByTickets_id(ticketId);

        List<CommentResponse> ticketCommentResponse = new ArrayList<>();

        for (TicketComments comment : ticketComments) {
            ticketCommentResponse.add(ticketCommentsToCommentResponseConverter.convert(comment));
        }

        log.info("Fetched {} comments for ticket ID: {}", ticketCommentResponse.size(), ticketId);
        return ResponseEntity.ok(ticketCommentResponse);
    }
}
