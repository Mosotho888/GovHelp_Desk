package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Priority;
import com.loggingsystem.springjwtauth.service.PriorityService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/priority")
public class PriorityController {
    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping
    public ResponseEntity<List<Priority>> findAll(Pageable pageable) {
        return priorityService.findAll(pageable);
    }
}
