package za.gov.helpdesk.ticket.service;

import za.gov.helpdesk.employee.dto.EmployeeResponse;
import za.gov.helpdesk.employee.service.EmployeeToEmployeeResponseConverter;
import za.gov.helpdesk.ticket.dto.TicketResponse;
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
