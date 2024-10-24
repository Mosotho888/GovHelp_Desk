package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Priority;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PriorityRepository extends CrudRepository<Priority, Long>, PagingAndSortingRepository<Priority, Long> {
}
