package za.gov.helpdesk.category.service;

import java.util.List;

import za.gov.helpdesk.category.dto.request.CreateCategoryRequest;
import za.gov.helpdesk.category.dto.request.UpdateCategoryRequest;
import za.gov.helpdesk.category.dto.response.CategoryResponse;

public interface CategoryService {

    /** Returns every root category with its subcategories nested under {@code children}. */
    List<CategoryResponse> getCategoryTree(boolean activeOnly);

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request);

    /** Soft-deletes a category by marking it inactive; it stays on historical tickets. */
    void deactivateCategory(Long categoryId);
}
