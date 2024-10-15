package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "statuses")
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String status_name;

    public Status() {
    }

    public Status(Long id, String status_name) {
        this.id = id;
        this.status_name = status_name;
    }

}