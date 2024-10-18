package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Getter
@Setter
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
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "phone_number")
    private String phone_number;
    @Column(name = "role")
    private String role;

    public Employees() {
    }

    public Employees(Long id, String password, String firstName, String lastName, String email, String phoneNumber, String role) {
        this.id = id;
        this.password = password;
        this.first_name = firstName;
        this.last_name = lastName;
        this.email = email;
        this.phone_number = phoneNumber;
        this.role = role;

    }
}
