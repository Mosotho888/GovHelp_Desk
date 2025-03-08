package com.loggingsystem.springjwtauth.priority.service.impl;

import com.loggingsystem.springjwtauth.priority.exception.PriorityNotFoundException;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.priority.repository.PriorityRepository;
import com.loggingsystem.springjwtauth.priority.service.PriorityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriorityServiceImpl implements PriorityService {
    private final PriorityRepository priorityRepository;

    public PriorityServiceImpl(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    @Override
    public ResponseEntity<List<Priority>> getAllPriorities(Pageable pageable) {
        Page<Priority> page = priorityRepository.findAll(PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
        ));

        return ResponseEntity.ok(page.getContent());
    }

    @Override
    public ResponseEntity<Priority> getPriorityById(Long priorityId) {
        Priority priority = getPriority(priorityId);

        return ResponseEntity.ok(priority);
    }

    @Override
    public Priority getPriority(Long priorityId) {
        Optional<Priority> optionalPriority = priorityRepository.findById(priorityId);

        if (optionalPriority.isPresent()) {
            return optionalPriority.get();
        }

        throw new PriorityNotFoundException();
    }
}
