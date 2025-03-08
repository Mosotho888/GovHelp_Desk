package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.employee.service.EmployeeToEmployeeResponseConverter;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponse;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticketcomment.dto.CommentResponse;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import com.loggingsystem.springjwtauth.ticketcomment.service.TicketCommentsToCommentResponseConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TicketToTicketResponseConverter implements Converter<Tickets, TicketResponse> {
    private final EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter;
    private final TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter;

    public TicketToTicketResponseConverter(EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter, TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter) {
        this.employeeToEmployeeResponseConverter = employeeToEmployeeResponseConverter;
        this.ticketCommentsToCommentResponseConverter = ticketCommentsToCommentResponseConverter;
    }

    @Override
    public @NotNull TicketResponse convert(Tickets ticket) {
        EmployeeResponse employeeResponse = employeeToEmployeeResponseConverter.convert(ticket.getAssignedTechnician());
        List<CommentResponse> commentResponse = new ArrayList<>();

        for (TicketComments ticketComments : ticket.getComments()) {
            commentResponse.add(ticketCommentsToCommentResponseConverter.convert(ticketComments));
        }

        return new TicketResponse(ticket.getId(), employeeResponse, ticket.getStatus(),
                ticket.getDescription(), ticket.getOwnerEmail(), ticket.getCategory(), ticket.getPriority(), ticket.getCreatedAt(),
                ticket.getUpdatedAt(), commentResponse);
    }
}
