package com.loggingsystem.springjwtauth.priority.repository;

import com.loggingsystem.springjwtauth.priority.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PriorityRepository extends JpaRepository<Priority, Long>, PagingAndSortingRepository<Priority, Long> {
}
