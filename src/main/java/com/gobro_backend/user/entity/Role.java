package com.gobro_backend.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Platform role: ETUDIANT, FORMATEUR or ADMIN (see cahier des charges §6 -
 * Rôles utilisateurs). Drives both coarse-grained Spring Security
 * hasRole(...) checks (ROLE_ prefix added in CustomUserDetailsService)
 * and fine-grained hasAuthority(...) checks via its {@link Permission} set.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ETUDIANT | FORMATEUR | ADMIN */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role(String name) {
        this.name = name;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }
}