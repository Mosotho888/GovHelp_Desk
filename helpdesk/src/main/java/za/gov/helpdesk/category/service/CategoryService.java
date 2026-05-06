package za.gov.helpdesk.category.service;

import za.gov.helpdesk.category.dto.TicketsByCategoryResponse;
import za.gov.helpdesk.category.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;


public interface CategoryService {
    ResponseEntity<Page<Category>> getAllCategories(Pageable pageable);
    ResponseEntity<Category> getCategoryById (Long categoryId);
    ResponseEntity<Category> updateCategoryById(Category updatedCategory, Long categoryId);
    ResponseEntity<Void> deleteCategoryById(Long categoryId);
    ResponseEntity<TicketsByCategoryResponse> getAllTicketsByCategoryId(Long categoryId);
    ResponseEntity<Void> createCategory(Category newCategory);
    Category getCategory(Long categoryId);
}
