package com.loggingsystem.springjwtauth.priority.controller;

import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.priority.service.PriorityService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/priority")
public class PriorityController {
    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping
    public ResponseEntity<List<Priority>> findAllPriorities(Pageable pageable) {
        return priorityService.getAllPriorities(pageable);
    }
}
