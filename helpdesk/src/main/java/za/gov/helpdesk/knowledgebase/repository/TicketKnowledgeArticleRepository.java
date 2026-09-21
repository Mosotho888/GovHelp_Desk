package za.gov.helpdesk.knowledgebase.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.knowledgebase.model.TicketKnowledgeArticle;

@Repository
public interface TicketKnowledgeArticleRepository
        extends JpaRepository<TicketKnowledgeArticle, Long> {

    List<TicketKnowledgeArticle> findByTicketIdOrderByLinkedAtDesc(Long ticketId);

    Page<TicketKnowledgeArticle> findByArticleIdOrderByLinkedAtDesc(
            Long articleId, Pageable pageable);

    Optional<TicketKnowledgeArticle> findByTicketIdAndArticleId(Long ticketId, Long articleId);

    boolean existsByTicketIdAndArticleId(Long ticketId, Long articleId);
}
