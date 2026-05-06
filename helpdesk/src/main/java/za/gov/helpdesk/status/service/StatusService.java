package za.gov.helpdesk.status.service;

import za.gov.helpdesk.status.model.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StatusService {
    ResponseEntity<List<Status>> getAllStatus(Pageable pageable);
    ResponseEntity<Status> getStatusById(Long statusId);
    Status getStatus(Long statusId);
}
