package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.exception.PriorityNotFoundException;
import com.loggingsystem.springjwtauth.model.Priority;
import com.loggingsystem.springjwtauth.repository.PriorityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriorityService {
    private final PriorityRepository priorityRepository;

    public PriorityService(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    public ResponseEntity<List<Priority>> getAllPriorities(Pageable pageable) {
        Page<Priority> page = priorityRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page.getContent());
    }

    public ResponseEntity<Priority> getPriorityById(Long priorityId) {
        Priority priority = getPriority(priorityId);

        return ResponseEntity.ok(priority);
    }

    public Priority getPriority(Long priorityId) {
        Optional<Priority> optionalPriority = priorityRepository.findById(priorityId);

        if (optionalPriority.isPresent()) {
            return optionalPriority.get();
        }

        throw new PriorityNotFoundException();
    }
}
