package com.gobro_backend.user.repository;



import com.gobro_backend.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Role} (ETUDIANT, FORMATEUR, ADMIN).
 *
 * Used by:
 *  - AuthService#register           -> resolves the role requested at signup
 *  - UserService#assignRole         -> admin changing a user's role
 *  - OAuth2AuthenticationSuccessHandler (indirectly, via just-in-time
 *    provisioning with a default role name)
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}