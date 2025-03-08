package com.loggingsystem.springjwtauth.status.service.impl;

import com.loggingsystem.springjwtauth.status.exception.StatusNotFoundException;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.status.repository.StatusRepository;
import com.loggingsystem.springjwtauth.status.service.StatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StatusServiceImpl implements StatusService {
    private final StatusRepository statusRepository;

    public StatusServiceImpl(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Override
    public ResponseEntity<List<Status>> getAllStatus(Pageable pageable) {
        Page<Status> page = statusRepository.findAll(PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "id"))
                ));

        return ResponseEntity.ok(page.getContent());
    }

    @Override
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
