package com.loggingsystem.springjwtauth.status.controller;

import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping
    public ResponseEntity<List<Status>> getAllStatues(Pageable pageable) {
        return statusService.getAllStatus(pageable);
    }
}
