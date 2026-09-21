package za.gov.helpdesk.knowledgebase.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@SuppressWarnings("PMD.TooManyFields")
public class ArticleResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private KnowledgeArticle.ArticleType type;
    private KnowledgeArticle.ArticleStatus status;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private Set<String> tags;
    private long viewCount;
    private int helpfulCount;
    private int notHelpfulCount;
    private int usageCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
