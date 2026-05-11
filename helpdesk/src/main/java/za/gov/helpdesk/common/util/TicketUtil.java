package za.gov.helpdesk.common.util;

import za.gov.helpdesk.ticket.dto.AssignedTicketsDTO;
import za.gov.helpdesk.ticket.dto.SubmittedTicketsDTO;
import za.gov.helpdesk.ticket.dto.TicketResponse;
import za.gov.helpdesk.ticket.dto.TicketsWithoutCategory;
import za.gov.helpdesk.ticket.exception.TicketNotFoundException;
import za.gov.helpdesk.ticket.model.Ticket;
import za.gov.helpdesk.ticket.repository.TicketRepository;
import za.gov.helpdesk.ticket.service.TicketToTicketResponseConverter;
import za.gov.helpdesk.ticket.service.TicketsToTicketsWithoutCategoryConverter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TicketUtil {
    private final TicketRepository ticketRepository;
    private final TicketToTicketResponseConverter ticketToTicketResponseConverter;
    private final TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter;

    public TicketUtil(TicketRepository ticketRepository, TicketToTicketResponseConverter ticketToTicketResponseConverter, TicketsToTicketsWithoutCategoryConverter ticketsToTicketsWithoutCategoryConverter) {
        this.ticketRepository = ticketRepository;
        this.ticketToTicketResponseConverter = ticketToTicketResponseConverter;
        this.ticketsToTicketsWithoutCategoryConverter = ticketsToTicketsWithoutCategoryConverter;
    }

    @NotNull
    public Ticket getTicket(Long ticketId) {
        log.info("Fetching ticket with ID: {}", ticketId);
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);

        if (optionalTicket.isPresent()) {
            log.info("Ticket found with ID: {}", ticketId);
            return optionalTicket.get();
        }

        log.warn("Ticket not found with ID: {}", ticketId);
        throw new TicketNotFoundException();
    }

    @NotNull
    public List<TicketResponse> mapToTicketResponse(List<Ticket> tickets) {
        List<TicketResponse> ticketResponse = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticketResponse.add(ticketToTicketResponseConverter.convert(ticket));
        }
        return ticketResponse;
    }

    @NotNull
    public List<TicketsWithoutCategory> mapToTicketsByCategoryResponse(List<Ticket> tickets) {
        List<TicketsWithoutCategory> ticketsByCategoryResponseList = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticketsByCategoryResponseList.add(ticketsToTicketsWithoutCategoryConverter.convert(ticket));
        }
        return ticketsByCategoryResponseList;
    }

    @NotNull
    public List<AssignedTicketsDTO> mapToTicketAssignedDTO(List<TicketResponse> page) {
        return page
                .stream()
                .map(AssignedTicketsDTO::new)
                .toList();
    }

    @NotNull
    public List<SubmittedTicketsDTO> mapToSubmittedTicketsDTO(List<TicketResponse> page) {
        return page
                .stream()
                .map(SubmittedTicketsDTO::new)
                .collect(Collectors.toList());
    }

    @NotNull
    public List<AssignedTicketsDTO> getAssignedTickets(za.gov.helpdesk.users.model.User employee) {
        List<Ticket> tickets = ticketRepository.findAllByAssignedTechnician(employee);

        List<TicketResponse> ticketResponse = mapToTicketResponse(tickets);

        return mapToTicketAssignedDTO(ticketResponse);
    }

    @NotNull
    public List<SubmittedTicketsDTO> getTicketsByOwner(String email) {
        List<Ticket> tickets = ticketRepository.findAllByOwnerEmail(email);

        List<TicketResponse> ticketResponse = mapToTicketResponse(tickets);

        return mapToSubmittedTicketsDTO(ticketResponse);

    }
}
