package za.gov.helpdesk.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.comment.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, PagingAndSortingRepository<Comment, Long> {

    Page<Comment> findByTicketId(Long ticketId, Pageable pageable);

    List<Comment> findByParentId(Long parentId);

    long countByTicketId(Long ticketId);
}
