package za.gov.helpdesk.ticket.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.agent.model.Agent;
import za.gov.helpdesk.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import za.gov.helpdesk.users.model.User;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

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

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.id = :ticketId
        AND (
            :role = 'ADMIN'
            OR (:role = 'AGENT'
                AND (t.assignee IS NULL
                     OR t.assignee.user.email = :email))
            OR (:role = 'USER'
                AND t.requester.email = :email)
        )
    """)
    Optional<Ticket> findByIdAndPrincipal(
            @Param("ticketId") Long ticketId,
            @Param("email")    String email,
            @Param("role")     String role
    );

    // For end users: only their own tickets
    Page<Ticket> findByRequester(User requester, Pageable pageable);

    Long countByAssigneeIdAndStatus(Long assigneeId, Ticket.Status status);

    @Modifying
    @Query("UPDATE Ticket t SET t.assignee = null " +
            "WHERE t.assignee = :agent " +
            "AND t.status NOT IN (za.gov.helpdesk.ticket.model.Ticket.Status.CLOSED, " +
            "za.gov.helpdesk.ticket.model.Ticket.Status.RESOLVED)")
    int unassignFromAgent(@Param("agent") Agent agent);
}
