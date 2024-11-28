package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Status;
import com.loggingsystem.springjwtauth.service.StatusService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/status")
public class StatusController {
    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public ResponseEntity<List<Status>> findAllStatues(Pageable pageable) {
        return statusService.findAll(pageable);
    }
}
