package com.loggingsystem.springjwtauth.repository;

import com.loggingsystem.springjwtauth.model.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface RoleRepository extends CrudRepository<Role, Long>, PagingAndSortingRepository<Role, Long> {
}
