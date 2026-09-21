package za.gov.helpdesk.knowledgebase.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Size;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateArticleRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    private String summary;

    private String content;

    private KnowledgeArticle.ArticleType type;

    /**
     * Setting this transitions the article's lifecycle state (DRAFT -&gt; PUBLISHED -&gt; ARCHIVED,
     * or back to PUBLISHED to republish an archived article). {@code publishedAt} is stamped the
     * first time an article reaches PUBLISHED and never overwritten after that, so republishing
     * doesn't erase when it was originally released.
     */
    private KnowledgeArticle.ArticleStatus status;

    private Long categoryId;

    /**
     * Explicitly clears the category link - a plain {@code null} on {@link #categoryId} is
     * indistinguishable from "field omitted" over JSON, the same reasoning behind {@code
     * UpdateAssetRequest.clearAssignedUser}.
     */
    private boolean clearCategory;

    /** Replaces the article's full tag set when provided. */
    private Set<String> tags;
}
