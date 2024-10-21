package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface StatusRepository extends CrudRepository<Status, Long>, PagingAndSortingRepository<Status, Long> {
    //Page<Status> findAll(PageRequest pageRequest);
}
