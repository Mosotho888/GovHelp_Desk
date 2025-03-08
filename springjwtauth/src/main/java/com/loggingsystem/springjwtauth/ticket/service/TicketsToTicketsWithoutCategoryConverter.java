package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.employee.dto.EmployeeResponse;
import com.loggingsystem.springjwtauth.employee.service.EmployeeToEmployeeResponseConverter;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsWithoutCategory;
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
public class TicketsToTicketsWithoutCategoryConverter implements Converter<Tickets, TicketsWithoutCategory> {
    private final EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter;
    private final TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter;

    public TicketsToTicketsWithoutCategoryConverter(EmployeeToEmployeeResponseConverter employeeToEmployeeResponseConverter, TicketCommentsToCommentResponseConverter ticketCommentsToCommentResponseConverter) {
        this.employeeToEmployeeResponseConverter = employeeToEmployeeResponseConverter;
        this.ticketCommentsToCommentResponseConverter = ticketCommentsToCommentResponseConverter;
    }

    @Override
    public @NotNull TicketsWithoutCategory convert(Tickets ticket) {
        EmployeeResponse technicianResponse = employeeToEmployeeResponseConverter.convert(ticket.getAssignedTechnician());
        List<CommentResponse> commentsResponse = new ArrayList<>();

        for (TicketComments comment : ticket.getComments()) {
            commentsResponse.add(ticketCommentsToCommentResponseConverter.convert(comment));
        }


        return new TicketsWithoutCategory(
                ticket.getId(),
                ticket.getOwnerEmail(),
                technicianResponse,
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getDescription(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                commentsResponse);
    }
}
