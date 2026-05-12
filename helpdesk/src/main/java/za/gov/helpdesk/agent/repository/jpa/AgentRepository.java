package za.gov.helpdesk.agent.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.agent.model.Agent;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
