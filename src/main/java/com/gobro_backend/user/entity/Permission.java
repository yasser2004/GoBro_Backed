package com.gobro_backend.user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fine-grained authority (e.g. COURSE_CREATE, COURSE_VALIDATE, USER_MANAGE,
 * PAYMENT_MANAGE, BADGE_MANAGE) attached to one or more {@link Role}s.
 * Consumed by CustomUserDetailsService to build Spring Security authorities,
 * enabling @PreAuthorize("hasAuthority('...')") checks in addition to
 * the coarser hasRole('ADMIN'|'FORMATEUR'|'ETUDIANT').
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public Permission(String name) {
        this.name = name;
    }
}