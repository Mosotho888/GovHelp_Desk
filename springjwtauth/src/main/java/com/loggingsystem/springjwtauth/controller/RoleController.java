package com.loggingsystem.springjwtauth.controller;

import com.loggingsystem.springjwtauth.model.Role;
import com.loggingsystem.springjwtauth.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<Role>> findAllRoles(Pageable pageable) {
        return roleService.findAll(pageable);
    }
}
