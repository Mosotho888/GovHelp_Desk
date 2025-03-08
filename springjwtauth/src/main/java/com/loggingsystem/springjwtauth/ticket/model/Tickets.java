package com.loggingsystem.springjwtauth.ticket.model;

import com.loggingsystem.springjwtauth.employee.model.Employees;
import com.loggingsystem.springjwtauth.category.model.Category;
import com.loggingsystem.springjwtauth.priority.model.Priority;
import com.loggingsystem.springjwtauth.status.model.Status;
import com.loggingsystem.springjwtauth.ticketcomment.model.TicketComments;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "owner")
    private String ownerEmail;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "priority_id")
    private Priority priority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "tickets", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TicketComments> comments = new ArrayList<>();

    public Tickets() {
    }

}
