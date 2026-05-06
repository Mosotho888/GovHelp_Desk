package za.gov.helpdesk.emailnotification.repository;

import za.gov.helpdesk.emailnotification.model.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
}
