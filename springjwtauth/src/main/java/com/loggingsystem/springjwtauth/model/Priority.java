package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "priority")
public class Priority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "level")
    private String level;
    @Column(name = "description")
    private String description;

    public Priority() {
    }

    public Priority(Long id, String level, String description) {
        this.id = id;
        this.level = level;
        this.description = description;
    }

}
