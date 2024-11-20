package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.TicketComments;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketCommentsRepository extends CrudRepository<TicketComments, Long>, PagingAndSortingRepository<TicketComments, Long> {
    List<TicketComments> findAllByTickets_id(Long id);
}
