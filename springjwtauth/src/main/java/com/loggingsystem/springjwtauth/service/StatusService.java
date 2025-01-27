package com.loggingsystem.springjwtauth.service;

import com.loggingsystem.springjwtauth.exception.StatusNotFoundException;
import com.loggingsystem.springjwtauth.model.Status;
import com.loggingsystem.springjwtauth.repository.StatusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public ResponseEntity<Status> getStatusById(Long statusId) {
        Status status = getStatus(statusId);

        return ResponseEntity.ok(status);
    }

    public Status getStatus(Long statusId) {
        Optional<Status> optionalStatus = statusRepository.findById(statusId);

        if (optionalStatus.isPresent()) {
            return optionalStatus.get();
        }

        throw new StatusNotFoundException();
    }
}
