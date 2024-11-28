package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Employees;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface EmployeesRepository extends CrudRepository<Employees, Long>, PagingAndSortingRepository<Employees, Long> {
    Optional<Employees> findByEmail(String email);
    Page<Employees> findAllByRole(String role, PageRequest pageRequest);
}
