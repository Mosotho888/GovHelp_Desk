package com.loggingsystem.springjwtauth.emailnotification.repository;

import com.loggingsystem.springjwtauth.emailnotification.model.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
}
