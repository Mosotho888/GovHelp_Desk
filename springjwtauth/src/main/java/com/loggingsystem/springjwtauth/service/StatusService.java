package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.model.Status;
import com.loggingsystem.springjwtauth.repository.StatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatusService {
    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public ResponseEntity<List<Status>> getAllStatus(Pageable pageable) {
        Page<Status> page = statusRepository.findAll(PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
                ));

        return ResponseEntity.ok(page.getContent());
    }
}
