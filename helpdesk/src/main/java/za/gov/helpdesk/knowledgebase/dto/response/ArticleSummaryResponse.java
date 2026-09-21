package za.gov.helpdesk.knowledgebase.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Lighter than {@link ArticleResponse} - omits the full content body, for list/search views. */
@Setter
@Getter
@Builder
public class ArticleSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private KnowledgeArticle.ArticleType type;
    private KnowledgeArticle.ArticleStatus status;
    private String categoryName;
    private Set<String> tags;
    private String authorName;
    private long viewCount;
    private int helpfulCount;
    private int notHelpfulCount;
    private int usageCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
