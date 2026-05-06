package za.gov.helpdesk.ticketcomment.repository;

import za.gov.helpdesk.ticketcomment.model.TicketComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketCommentsRepository extends JpaRepository<TicketComments, Long>, PagingAndSortingRepository<TicketComments, Long> {
    List<TicketComments> findAllByTickets_id(Long id);
}
