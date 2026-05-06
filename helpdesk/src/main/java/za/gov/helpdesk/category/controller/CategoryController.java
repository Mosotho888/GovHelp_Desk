package za.gov.helpdesk.category.controller;

import za.gov.helpdesk.category.dto.TicketsByCategoryResponse;
import za.gov.helpdesk.category.model.Category;
import za.gov.helpdesk.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/category")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(@RequestBody Category newCategory) {

        return categoryService.createCategory(newCategory);
    }
    @GetMapping
    public ResponseEntity<Page<Category>> getAllCategories(Pageable pageable) {
        return categoryService.getAllCategories(pageable);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<Category> updateCategoryById(@PathVariable Long categoryId, @RequestBody Category updatedCategory) {
        return categoryService.updateCategoryById(updatedCategory, categoryId);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategoryById(@PathVariable Long categoryId) {
        return categoryService.deleteCategoryById(categoryId);
    }

    @GetMapping("/{categoryId}/tickets")
    public ResponseEntity<TicketsByCategoryResponse> getAllTicketsByCategoryId(@PathVariable Long categoryId) {
        return categoryService.getAllTicketsByCategoryId(categoryId);
    }
    

}
