package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.User;

@Setter
@Getter
@Entity
@Table(name = "Tickets")
public class Tickets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private Employees assigned_technician;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "comments")
    private String comment;

    @Column(name = "resolution")
    private String resolution;

    @Column(name = "attachments")
    private String attachments;

    @Column(name = "owner")
    private String owner;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "priority_id")
    private Priority priority;

    public Tickets() {
    }

}
