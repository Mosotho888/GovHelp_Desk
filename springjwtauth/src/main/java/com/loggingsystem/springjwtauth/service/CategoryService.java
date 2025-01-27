package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.exception.CategoryNotFoundException;
import com.loggingsystem.springjwtauth.model.Category;
import com.loggingsystem.springjwtauth.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ResponseEntity<List<Category>> getAllCategories(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page.getContent());
    }

    public ResponseEntity<Category> getCategoryById (Long categoryId) {
        Category category = getCategory(categoryId);

        return  ResponseEntity.ok(category);
    }

    public Category getCategory(Long categoryId) {
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);

        if (optionalCategory.isPresent()) {
            return optionalCategory.get();
        }

        throw new CategoryNotFoundException();
    }
}
