package com.loggingsystem.springjwtauth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private Employees assignedTechnician;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "description")
    private String description;

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

    @Column(name = "created_at")
    private LocalDateTime created_at;

    @Column(name = "updated_at")
    private LocalDateTime updated_at;

    @OneToMany(mappedBy = "tickets", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketComments> comments = new ArrayList<>();

    public Tickets() {
    }

}
