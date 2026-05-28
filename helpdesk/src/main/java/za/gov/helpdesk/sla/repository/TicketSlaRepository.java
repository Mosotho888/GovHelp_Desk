package za.gov.helpdesk.sla.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import za.gov.helpdesk.sla.model.TicketSla;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketSlaRepository extends JpaRepository<TicketSla, Long> {

    Optional<TicketSla> findByTicketId(Long ticketId);

    // Tickets whose response deadline is approaching and warning not yet sent
    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.firstResponseAt IS NULL
          AND s.responseWarningSent = false
          AND s.responseDueAt BETWEEN :now AND :threshold
    """)
    List<TicketSla> findResponseWarningsDue(LocalDateTime now, LocalDateTime threshold);

    // Tickets whose resolution deadline is approaching and warning not yet sent
    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.resolvedAt IS NULL
          AND s.resolutionWarningSent = false
          AND s.resolutionDueAt BETWEEN :now AND :threshold
    """)
    List<TicketSla> findResolutionWarningsDue(LocalDateTime now, LocalDateTime threshold);

    // All unresolved SLAs past their deadline — for breach detection
    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.resolvedAt IS NULL
          AND s.resolutionBreached = false
          AND s.resolutionDueAt < :now
    """)
    List<TicketSla> findUnmarkedResolutionBreaches(LocalDateTime now);

    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.firstResponseAt IS NULL
          AND s.responseBreached = false
          AND s.responseDueAt < :now
    """)
    List<TicketSla> findUnmarkedResponseBreaches(LocalDateTime now);
}
