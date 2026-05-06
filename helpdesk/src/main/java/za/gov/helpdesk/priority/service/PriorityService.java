package za.gov.helpdesk.priority.service;

import za.gov.helpdesk.priority.model.Priority;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PriorityService {
    ResponseEntity<List<Priority>> getAllPriorities(Pageable pageable);
    ResponseEntity<Priority> getPriorityById(Long priorityId);
    Priority getPriority(Long priorityId);
}
