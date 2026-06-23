package za.gov.helpdesk.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.outbox.model.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
                name = "jakarta.persistence.lock.timeout",
                value = "-2"), // Translate to NOWAIT / SKIP LOCKED
    })
    @Query(
            value =
                    """
                    SELECT e.id FROM OutboxEvent e
                    WHERE e.status = 'PENDING'
                    ORDER BY e.createdAt ASC
                    """)
    List<Long> findNextPendingIds(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PROCESSED' AND e.processedAt < :before")
    int deleteProcessedBefore(@Param("before") LocalDateTime before);

    long countByStatus(OutboxEvent.Status status);
}
