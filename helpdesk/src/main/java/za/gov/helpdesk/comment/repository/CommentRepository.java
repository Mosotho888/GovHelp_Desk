package za.gov.helpdesk.comment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.comment.model.Comment;

@Repository
public interface CommentRepository
        extends JpaRepository<Comment, Long>, PagingAndSortingRepository<Comment, Long> {

    @Query(
            """
            SELECT c FROM Comment c
            WHERE c.ticket.id = :ticketId
              AND c.parent IS NULL
              AND (
                  :role IN ('ADMIN','AGENT')
                  OR (
                      :role = 'USER'
                      AND c.ticket.requester.email = :email
                      AND c.internal = false
                  )
              )
            ORDER BY c.createdAt ASC
            """)
    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findVisibleByTicketId(
            @Param("ticketId") Long ticketId,
            @Param("email") String email,
            @Param("role") String role,
            Pageable pageable);

    @Query(
            """
            SELECT c FROM Comment c
            WHERE c.parent.id = :parentId
              AND (
                  :role IN ('ADMIN','AGENT')
                  OR (:role = 'USER' AND c.internal = false)
              )
            ORDER BY c.createdAt ASC
            """)
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findVisibleReplies(@Param("parentId") Long parentId, @Param("role") String role);

    @Query(
            """
            SELECT c FROM Comment c
            WHERE c.id = :commentId
              AND (
                  :role IN ('ADMIN','AGENT')
                  OR (:role = 'USER'
                      AND c.ticket.requester.email = :email
                      AND c.internal = false)
              )
            """)
    @EntityGraph(attributePaths = {"author", "ticket"})
    Optional<Comment> findByIdForActor(
            @Param("commentId") Long commentId,
            @Param("email") String email,
            @Param("role") String role);

    long countByTicketId(Long ticketId);
}
