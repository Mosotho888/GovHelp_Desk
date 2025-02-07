package com.loggingsystem.springjwtauth.priority.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "level is required")
    @Column(name = "level")
    private String level;
    @NotNull(message = "description is required")
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
