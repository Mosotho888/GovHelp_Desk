package za.gov.helpdesk.comment.repository;

import za.gov.helpdesk.comment.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketCommentsRepository extends JpaRepository<Comment, Long>, PagingAndSortingRepository<Comment, Long> {
    List<Comment> findAllByTickets_id(Long id);
}
