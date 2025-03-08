package com.loggingsystem.springjwtauth.category.service.impl;

import com.loggingsystem.springjwtauth.category.dto.TicketsByCategoryResponse;
import com.loggingsystem.springjwtauth.category.exception.CategoryAlreadyExistException;
import com.loggingsystem.springjwtauth.category.exception.CategoryNotFoundException;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.category.repository.CategoryRepository;
import com.loggingsystem.springjwtauth.category.service.CategoryService;
import com.loggingsystem.springjwtauth.category.service.TicketsByCategoryConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final TicketsByCategoryConverter ticketsByCategoryConverter;

    public CategoryServiceImpl(CategoryRepository categoryRepository, TicketsByCategoryConverter ticketsByCategoryConverter) {
        this.categoryRepository = categoryRepository;
        this.ticketsByCategoryConverter = ticketsByCategoryConverter;
    }

    // focus on paging
    @Override
    public ResponseEntity<Page<Category>> getAllCategories(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page);
    }

    @Override
    public ResponseEntity<Category> getCategoryById (Long categoryId) {
        Category category = getCategory(categoryId);

        return  ResponseEntity.ok(category);
    }

    @Override
    public Category getCategory(Long categoryId) {
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);

        if (optionalCategory.isPresent()) {
            return optionalCategory.get();
        }

        throw new CategoryNotFoundException();
    }

    @Override
    public ResponseEntity<Category> updateCategoryById(Category updatedCategory, Long categoryId) {
        Category category = getCategory(categoryId);

        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());

        categoryRepository.save(category);

        return ResponseEntity.ok(category);
    }

    @Override
    public ResponseEntity<Void> deleteCategoryById(Long categoryId) {
        Category category = getCategory(categoryId);

        categoryRepository.deleteById(category.getId());

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TicketsByCategoryResponse> getAllTicketsByCategoryId(Long categoryId) {
        Category category = getCategory(categoryId);

        TicketsByCategoryResponse ticketsByCategoryResponse = ticketsByCategoryConverter.convert(category);

        return ResponseEntity.ok(ticketsByCategoryResponse);

    }

    @Override
    public ResponseEntity<Void> createCategory(Category newCategory) {
        boolean doesCategoryExist = categoryRepository.existsByName(newCategory.getName());

        if (doesCategoryExist) {
            throw new CategoryAlreadyExistException();
        }

        Category category = new Category();
        category.setName(newCategory.getName());
        category.setDescription(newCategory.getDescription());

        categoryRepository.save(category);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
