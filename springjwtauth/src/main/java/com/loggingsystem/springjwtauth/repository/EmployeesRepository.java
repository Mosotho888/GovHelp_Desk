package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Employees;
import org.springframework.data.repository.CrudRepository;

public interface EmployeesRepository extends CrudRepository<Employees, Integer> {
}
