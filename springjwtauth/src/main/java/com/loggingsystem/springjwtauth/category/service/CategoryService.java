package com.loggingsystem.springjwtauth.category.service;

import com.loggingsystem.springjwtauth.category.dto.TicketsByCategoryIdResponseDTO;
import com.loggingsystem.springjwtauth.category.exception.CategoryAlreadyExistException;
import com.loggingsystem.springjwtauth.category.exception.CategoryNotFoundException;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.category.repository.CategoryRepository;
import com.loggingsystem.springjwtauth.ticket.dto.TicketResponseDTO;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsByCategoryResponse;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import com.loggingsystem.springjwtauth.ticket.repository.TicketsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TicketsRepository ticketsRepository;

    public CategoryService(CategoryRepository categoryRepository, TicketsRepository ticketsRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketsRepository = ticketsRepository;
    }

    // focus on paging
    public ResponseEntity<Page<Category>> getAllCategories(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page);
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

    public ResponseEntity<Category> updateCategoryById(Category updatedCategory, Long categoryId) {
        Category category = getCategory(categoryId);

        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());

        categoryRepository.save(category);

        return ResponseEntity.ok(category);
    }

    public ResponseEntity<Void> deleteCategoryById(Long categoryId) {
        Category category = getCategory(categoryId);

        categoryRepository.deleteById(category.getId());

        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<TicketsByCategoryIdResponseDTO> getAllTicketsByCategoryId(Long categoryId) {
        Category category = getCategory(categoryId);
        List<Tickets> tickets = ticketsRepository.findAllByCategory(category);

        List<TicketResponseDTO> ticketResponseDTO = tickets.stream().map(TicketResponseDTO::new).toList();

        TicketsByCategoryIdResponseDTO ticketsByCategoryIdResponseDTO = new TicketsByCategoryIdResponseDTO(category, ticketResponseDTO);

        return ResponseEntity.ok(ticketsByCategoryIdResponseDTO);

    }

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
