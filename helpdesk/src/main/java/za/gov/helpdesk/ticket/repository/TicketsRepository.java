package za.gov.helpdesk.ticket.repository;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketsRepository extends JpaRepository<Ticket, Long>, PagingAndSortingRepository<Ticket, Long> {
    List<Ticket> findAllByAssignedTechnician(za.gov.helpdesk.users.model.User employee);
    List<Ticket> findAllByOwnerEmail(String email);
    List<Ticket> findAllByCategory(Category category);
}
