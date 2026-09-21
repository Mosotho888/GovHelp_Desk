package za.gov.helpdesk.knowledgebase.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticleFeedback;

@Repository
public interface KnowledgeArticleFeedbackRepository
        extends JpaRepository<KnowledgeArticleFeedback, Long> {

    Optional<KnowledgeArticleFeedback> findByArticleIdAndUserId(Long articleId, Long userId);
}
