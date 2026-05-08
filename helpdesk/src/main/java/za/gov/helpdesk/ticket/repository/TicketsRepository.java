package za.gov.helpdesk.ticket.repository;

import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.model.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketsRepository extends JpaRepository<Tickets, Long>, PagingAndSortingRepository<Tickets, Long> {
    List<Tickets> findAllByAssignedTechnician(za.gov.helpdesk.users.model.User employee);
    List<Tickets> findAllByOwnerEmail(String email);
    List<Tickets> findAllByCategory(Category category);
}
