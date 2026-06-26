package za.gov.helpdesk.ticket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import za.gov.helpdesk.ticket.dto.response.TicketResponse;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.users.mapper.UserMapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface TicketMapper {

    @Mapping(source = "assignee.user", target = "assignee")
    TicketResponse toTicketResponse(Ticket ticket);
}
