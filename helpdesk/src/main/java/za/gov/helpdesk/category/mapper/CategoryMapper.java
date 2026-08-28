package za.gov.helpdesk.category.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import za.gov.helpdesk.category.dto.response.CategoryResponse;
import za.gov.helpdesk.category.dto.response.CategorySummaryResponse;
import za.gov.helpdesk.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toFlatResponse(Category category);

    List<CategoryResponse> toFlatResponseList(List<Category> categories);

    /**
     * Builds the ticket-facing summary. {@code path} is assembled by the caller (see {@code
     * TicketCategoryQueryHelper#buildPath}) since it depends on the ancestor chain, not just the
     * single entity MapStruct sees here.
     */
    default CategorySummaryResponse toSummaryResponse(final Category category, final String path) {
        if (category == null) {
            return null;
        }
        return CategorySummaryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .path(path)
                .build();
    }
}
