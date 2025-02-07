package com.loggingsystem.springjwtauth.ticket.repository;

import com.loggingsystem.springjwtauth.category.dto.TicketsByCategoryIdResponseDTO;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.ticket.dto.TicketsByCategoryResponse;
import com.loggingsystem.springjwtauth.ticket.model.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketsRepository extends JpaRepository<Tickets, Long>, PagingAndSortingRepository<Tickets, Long> {
    List<Tickets> findAllByAssignedTechnician(Employees employee);
    List<Tickets> findAllByOwner(String email);
    List<Tickets> findAllByCategory(Category category);
}
