package com.loggingsystem.springjwtauth.category.controller;

import com.loggingsystem.springjwtauth.category.dto.TicketsByCategoryIdResponseDTO;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<TicketsByCategoryIdResponseDTO> getAllTicketsByCategoryId(@PathVariable Long categoryId) {
        return categoryService.getAllTicketsByCategoryId(categoryId);
    }
    

}
