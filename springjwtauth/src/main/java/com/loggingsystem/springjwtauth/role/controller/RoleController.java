package com.loggingsystem.springjwtauth.role.controller;

import com.loggingsystem.springjwtauth.role.model.Role;
import com.loggingsystem.springjwtauth.role.service.RoleService;
import com.loggingsystem.springjwtauth.role.service.impl.RoleServiceImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
@RestController
@RequestMapping("/api/role")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles(Pageable pageable) {
        return roleService.getAllRoles(pageable);
    }
}
