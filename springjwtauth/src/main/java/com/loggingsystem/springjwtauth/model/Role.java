package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
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
    @Column(nullable = false, unique = true)
    private String role_name;

    public Role() {
    }

    public Role(Long id, String role_name) {
        this.id = id;
        this.role_name = role_name;
    }

}
