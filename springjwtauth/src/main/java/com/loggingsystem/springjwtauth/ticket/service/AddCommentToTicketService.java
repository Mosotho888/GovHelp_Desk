package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.common.util.TicketUtils;
import com.loggingsystem.springjwtauth.emailnotification.dto.EmailNotificationDTO;
import com.loggingsystem.springjwtauth.emailnotification.service.MessageSender;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.employee.service.EmployeesServices;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import com.loggingsystem.springjwtauth.ticketcomment.repository.TicketCommentsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@Slf4j
public class AddCommentToTicketService {

    private final EmployeesServices employeesService;
    private final TicketCommentsRepository ticketCommentsRepository;
    private final TicketUtils ticketUtils;
    private final MessageSender messageSender;

    public AddCommentToTicketService(EmployeesServices employeesService, TicketCommentsRepository ticketCommentsRepository, TicketUtils ticketUtils, MessageSender messageSender) {
        this.employeesService = employeesService;
        this.ticketCommentsRepository = ticketCommentsRepository;
        this.ticketUtils = ticketUtils;
        this.messageSender = messageSender;
    }

    public ResponseEntity<Void> addCommentToTicket(Long ticketId, TicketComments comments, Principal principal) {
        log.info("Adding comment to ticket ID: {} by user: {}", ticketId, principal.getName());
        Tickets ticket = ticketUtils.getTicket(ticketId);
        Employees employee = employeesService.getEmployeeByEmail(principal.getName());

        comments.setTickets(ticket);
        comments.setCommenter(employee);

        ticketCommentsRepository.save(comments);

        EmailNotificationDTO emailRequest = new EmailNotificationDTO(ticket, comments.getComment());
        messageSender.sendTicketCommentMessage(emailRequest);

        log.info("Email notification request sent for comment: {}", comments.getId());

        log.info("Comment added successfully to ticket ID: {}", ticketId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
