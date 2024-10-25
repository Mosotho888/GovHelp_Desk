package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.TicketComments;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TicketCommentsRepository extends CrudRepository<TicketComments, Long>, PagingAndSortingRepository<TicketComments, Long> {
}
