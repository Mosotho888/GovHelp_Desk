package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.dto.CommentResponseDTO;
import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.model.TicketComments;
import com.loggingsystem.springjwtauth.model.Tickets;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface TicketsRepository extends CrudRepository<Tickets, Long>, PagingAndSortingRepository<Tickets, Long> {
    List<Tickets> findAllByAssignedTechnician(Employees employee);
}
