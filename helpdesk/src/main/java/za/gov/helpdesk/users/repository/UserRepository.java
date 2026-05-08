package za.gov.helpdesk.users.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import za.gov.helpdesk.users.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<za.gov.helpdesk.users.model.User, Long>, PagingAndSortingRepository<za.gov.helpdesk.users.model.User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Page<za.gov.helpdesk.users.model.User> findAllByRole(String role, PageRequest pageRequest);
}
