package za.gov.helpdesk.employee.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employees implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "first name is required")
    @Column(name = "first_name")
    private String firstName;

    @NotNull(message = "last name is required")
    @Column(name = "last_name")
    private String lastName;

    @NotNull(message = "email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "pwd")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "login_attempts", nullable = false)
    @Builder.Default
    private int loginAttempts = 0;

    @Column(name= "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at",  nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── UserDetails implementation ────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    public enum Role {
        USER,
        AGENT,
        ADMIN
    }
}
