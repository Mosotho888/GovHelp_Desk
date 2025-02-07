package com.loggingsystem.springjwtauth.ticketcomment.repository;

import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TicketCommentsRepository extends JpaRepository<TicketComments, Long>, PagingAndSortingRepository<TicketComments, Long> {
    List<TicketComments> findAllByTickets_id(Long id);
}
