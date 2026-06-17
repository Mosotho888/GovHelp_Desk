package za.gov.helpdesk.sla.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import za.gov.helpdesk.sla.model.SlaPolicy;
import za.gov.helpdesk.ticket.model.Ticket;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(Ticket.Priority priority);
}
