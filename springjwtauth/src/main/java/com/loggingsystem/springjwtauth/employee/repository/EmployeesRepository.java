package com.loggingsystem.springjwtauth.employee.repository;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface EmployeesRepository extends JpaRepository<Employees, Long>, PagingAndSortingRepository<Employees, Long> {
    Optional<Employees> findByEmail(String email);
    Boolean existsByEmail(String email);
    Page<Employees> findAllByRole(String role, PageRequest pageRequest);
}
