package com.loggingsystem.springjwtauth.ticket.service;

import com.loggingsystem.springjwtauth.status.dto.StatusRequestDTO;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface UpdateStatusService {
    ResponseEntity<Void> updateStatus(Long ticketId, StatusRequestDTO statusId, Principal principal);
}
