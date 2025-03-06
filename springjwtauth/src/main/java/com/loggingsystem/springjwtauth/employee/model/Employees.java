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
    private String firstName;
    @NotNull(message = "last name is required")
    @Column(name = "last_name")
    private String lastName;
    @NotNull(message = "email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name= "created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "role")
    private String role;

    public Employees() {
    }

    public Employees(Long id, String password, String firstName, String lastName, String email, String phoneNumber, LocalDateTime createdAt, LocalDateTime updatedAt, String role) {
        this.id = id;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.role = role;

    }
}
