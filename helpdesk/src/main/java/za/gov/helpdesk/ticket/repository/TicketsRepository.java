package za.gov.helpdesk.ticket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import za.gov.helpdesk.users.model.User;

import java.util.List;

@Repository
public interface TicketsRepository extends JpaRepository<Ticket, Long> {

    // For agents/admins: filter by status and/or priority
    @Query("""
        SELECT t FROM Ticket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Ticket> findWithFilters(
            @Param("status") Ticket.Status status,
            @Param("priority") Ticket.Priority priority,
            @Param("assigneeId") Long assigneeId,
            Pageable pageable
    );

    // For end users: only their own tickets
    Page<Ticket> findByRequester(User requester, Pageable pageable);

    Long countByAssigneeIdAndStatus(Long assigneeId, Ticket.Status status);
}
