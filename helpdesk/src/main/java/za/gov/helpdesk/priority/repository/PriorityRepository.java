package za.gov.helpdesk.priority.repository;

import za.gov.helpdesk.priority.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PriorityRepository extends JpaRepository<Priority, Long>, PagingAndSortingRepository<Priority, Long> {
}
