package com.loggingsystem.springjwtauth.role.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "role name is required")
    @Column(nullable = false, unique = true)
    private String role_name;

    public Role() {
    }

    public Role(Long id, String role_name) {
        this.id = id;
        this.role_name = role_name;
    }

}
