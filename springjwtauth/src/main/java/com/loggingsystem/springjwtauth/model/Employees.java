package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "pwd")
    private String password;
    @Column(name = "first_name")
    private String first_name;
    @Column(name = "last_name")
    private String last_name;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "phone_number")
    private String phone_number;
    @Column(name = "role")
    private String role;
}
