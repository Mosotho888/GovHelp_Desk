package za.gov.helpdesk.knowledgebase.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import za.gov.helpdesk.knowledgebase.dto.request.ArticleFeedbackRequest;
import za.gov.helpdesk.knowledgebase.dto.request.CreateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.request.UpdateArticleRequest;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleFeedbackResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;
import za.gov.helpdesk.users.model.User;

public interface ArticleService {

    ArticleResponse createArticle(CreateArticleRequest request, User actor);

    /** Fetches an article by id, incrementing its view count and enforcing citizen visibility. */
    ArticleResponse getArticleById(Long articleId, User actor);

    ArticleResponse getArticleBySlug(String slug, User actor);

    Page<ArticleSummaryResponse> getArticles(
            KnowledgeArticle.ArticleStatus status,
            KnowledgeArticle.ArticleType type,
            Long categoryId,
            String tag,
            Pageable pageable,
            User actor);

    Page<ArticleSummaryResponse> searchArticles(String query, Pageable pageable, User actor);

    ArticleResponse updateArticle(Long articleId, UpdateArticleRequest request, User actor);

    /** Hard-deletes an article and its links/feedback. Admin-only, for genuinely bad content. */
    void deleteArticle(Long articleId, User actor);

    ArticleFeedbackResponse submitFeedback(
            Long articleId, ArticleFeedbackRequest request, User actor);

    ArticleFeedbackResponse getFeedback(Long articleId, User actor);
}
