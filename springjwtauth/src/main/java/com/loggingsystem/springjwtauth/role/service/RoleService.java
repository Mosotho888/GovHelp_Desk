package com.loggingsystem.springjwtauth.role.service;

import com.loggingsystem.springjwtauth.role.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RoleService {
    ResponseEntity<List<Role>> getAllRoles(Pageable pageable);
}
