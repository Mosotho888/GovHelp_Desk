package za.gov.helpdesk.attachment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.attachment.model.Attachment;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Query("""
           SELECT a FROM Attachment a
           WHERE a.id = :attachmentId
             AND (
                 :role = 'ADMIN'
                 OR (:role = 'AGENT'
                     AND (a.ticket.assignee IS NULL
                          OR a.ticket.assignee.user.email = :email))
                 OR (:role = 'USER'
                     AND a.ticket.requester.email = :email)
             )
           """)
    Optional<Attachment> findByIdForActor(@Param("attachmentId") Long attachmentId, @Param("email") String email,
            @Param("role") String role);

    List<Attachment> findByTicketId(Long ticketId);

    long countByTicketId(Long ticketId);
}
