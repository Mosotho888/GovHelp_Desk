package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StatusRepository extends JpaRepository<Status, Long>, PagingAndSortingRepository<Status, Long> {
    //Page<Status> findAll(PageRequest pageRequest);
}
