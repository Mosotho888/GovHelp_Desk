package za.gov.helpdesk.agent.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import za.gov.helpdesk.agent.model.Agent;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByUserId(Long userId);

    Optional<Agent> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserId(Long userId);

    long countByAvailability(Agent.Availability availability);

    /** Candidate pool for category-based auto-routing: agents on shift in a given department. */
    List<Agent> findByDepartmentAndAvailability(String department, Agent.Availability availability);
}
