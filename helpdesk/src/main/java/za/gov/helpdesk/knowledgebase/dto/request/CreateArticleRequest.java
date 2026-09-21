package za.gov.helpdesk.knowledgebase.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateArticleRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 500, message = "Summary must not exceed 500 characters")
    private String summary;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Article type is required")
    private KnowledgeArticle.ArticleType type;

    /** Optional link to the ticket category this article helps with. */
    private Long categoryId;

    private Set<String> tags;
}
