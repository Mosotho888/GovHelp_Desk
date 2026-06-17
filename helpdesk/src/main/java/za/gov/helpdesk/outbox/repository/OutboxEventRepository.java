package za.gov.helpdesk.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.outbox.model.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
                   SELECT e FROM OutboxEvent e
                   WHERE e.status = 'PENDING'
                   ORDER BY e.createdAt ASC
                   """)
    List<OutboxEvent> findNextPendingBatch(Pageable pageable);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PROCESSED' AND e.processedAt < :before")
    int deleteProcessedBefore(@Param("before") LocalDateTime before);

    long countByStatus(OutboxEvent.Status status);
}
