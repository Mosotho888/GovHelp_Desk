package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.employee.service.EmployeeToEmployeeResponseConverter;
import za.gov.helpdesk.ticket.dto.TicketsWithoutCategory;
import za.gov.helpdesk.ticket.model.Tickets;
import za.gov.helpdesk.ticketcomment.dto.CommentResponse;
import za.gov.helpdesk.ticketcomment.model.TicketComments;
import za.gov.helpdesk.ticketcomment.service.TicketCommentsToCommentResponseConverter;
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
