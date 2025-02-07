package com.loggingsystem.springjwtauth.status.repository;

import com.loggingsystem.springjwtauth.status.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StatusRepository extends JpaRepository<Status, Long>, PagingAndSortingRepository<Status, Long> {
    //Page<Status> findAll(PageRequest pageRequest);
}
