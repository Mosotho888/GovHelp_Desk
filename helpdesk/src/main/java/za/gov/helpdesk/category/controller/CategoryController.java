package za.gov.helpdesk.category.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.gov.helpdesk.category.dto.request.CreateCategoryRequest;
import za.gov.helpdesk.category.dto.request.UpdateCategoryRequest;
import za.gov.helpdesk.category.dto.response.CategoryResponse;
import za.gov.helpdesk.category.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Ticket Categories", description = "Hierarchical ticket category management")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get the full category tree")
    public ResponseEntity<List<CategoryResponse>> getCategoryTree(
            @RequestParam(defaultValue = "false") final boolean activeOnly) {
        return ResponseEntity.ok(categoryService.getCategoryTree(activeOnly));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category by id")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable final Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category or subcategory (Admin only)")
    public CategoryResponse createCategory(
            @Valid @RequestBody final CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rename, re-route, or (de)activate a category (Admin only)")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable final Long id, @Valid @RequestBody final UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a category (Admin only). Historical tickets keep the label.")
    public void deactivateCategory(@PathVariable final Long id) {
        categoryService.deactivateCategory(id);
    }
}
