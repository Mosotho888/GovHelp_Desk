package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Employees;
import com.loggingsystem.springjwtauth.model.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketsRepository extends JpaRepository<Tickets, Long>, PagingAndSortingRepository<Tickets, Long> {
    List<Tickets> findAllByAssignedTechnician(Employees employee);
}
