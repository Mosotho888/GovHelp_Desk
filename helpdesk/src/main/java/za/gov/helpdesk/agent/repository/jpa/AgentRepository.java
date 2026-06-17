package za.gov.helpdesk.agent.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.gov.helpdesk.agent.model.Agent;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUserId(Long userId);

    Optional<Agent> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);
}
