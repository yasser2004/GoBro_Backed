package com.gobro_backend.user.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Core account entity, shared by all user types (Étudiant, Formateur, Admin) —
 * the concrete role/permissions are carried by {@link Role} rather than
 * subclassing, since a single account only ever holds one role at a time.
 *
 * Auditing fields (createdAt/updatedAt/createdBy/updatedBy) are populated
 * automatically by Spring Data JPA auditing, enabled in JpaConfig
 * (@EnableJpaAuditing, auditorAwareRef = "auditorProvider").
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt hash. Never exposed via DTOs. */
    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 1000)
    private String bio;

    @Column(length = 30)
    private String phoneNumber;

    /** LOCAL | GOOGLE | FACEBOOK — see OAuth2AuthenticationSuccessHandler. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String provider = "LOCAL";

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean twoFactorEnabled = false;

    /** TOTP secret, set by TwoFactorAuthService#generateSecret. Never exposed via DTOs. */
    @Column(length = 100)
    private String twoFactorSecret;

    // --- Gamification (see cahier des charges §8) ---

    @Column(nullable = false)
    @Builder.Default
    private int xp = 0;

    @Column(nullable = false)
    @Builder.Default
    private int level = 1;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false, length = 150)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 150)
    private String updatedBy;

    public void addXp(int amount) {
        this.xp += amount;
    }
}
