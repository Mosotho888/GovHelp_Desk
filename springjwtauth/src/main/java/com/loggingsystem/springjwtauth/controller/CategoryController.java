package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Category;
import com.loggingsystem.springjwtauth.service.CategoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCatetories(Pageable pageable) {
        return categoryService.getAllCategories(pageable);
    }
}
