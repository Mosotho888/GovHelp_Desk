package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Tickets;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TicketsRepository extends CrudRepository<Tickets, Long>, PagingAndSortingRepository<Tickets, Long> {
}
