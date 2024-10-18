package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Employees;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EmployeesRepository extends CrudRepository<Employees, Long> {
    Optional<Employees> findByEmail(String email);
}
