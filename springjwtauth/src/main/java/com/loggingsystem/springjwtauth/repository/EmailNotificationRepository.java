package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
}
