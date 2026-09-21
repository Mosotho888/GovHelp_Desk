package za.gov.helpdesk.knowledgebase.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import za.gov.helpdesk.knowledgebase.dto.response.ArticleResponse;
import za.gov.helpdesk.knowledgebase.dto.response.ArticleSummaryResponse;
import za.gov.helpdesk.knowledgebase.model.KnowledgeArticle;

@Mapper(componentModel = "spring")
public interface ArticleMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    ArticleResponse toArticleResponse(KnowledgeArticle article);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "authorName", source = "author.name")
    ArticleSummaryResponse toSummaryResponse(KnowledgeArticle article);
}
