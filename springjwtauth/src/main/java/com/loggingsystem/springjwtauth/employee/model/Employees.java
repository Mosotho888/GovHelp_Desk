package com.loggingsystem.springjwtauth.employee.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    @NotNull(message = "first name is required")
    @Column(name = "first_name")
    private String first_name;
    @NotNull(message = "last name is required")
    @Column(name = "last_name")
    private String last_name;
    @NotNull(message = "email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "phone_number")
    private String phone_number;
    @Column(name= "created_at")
    private LocalDateTime created_at;
    @Column(name="updated_at")
    private LocalDateTime updated_at;
    @Column(name = "role")
    private String role;

    public Employees() {
    }

    public Employees(Long id, String password, String firstName, String lastName, String email, String phoneNumber, LocalDateTime created_at, LocalDateTime updated_at, String role) {
        this.id = id;
        this.password = password;
        this.first_name = firstName;
        this.last_name = lastName;
        this.email = email;
        this.phone_number = phoneNumber;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.role = role;

    }
}
