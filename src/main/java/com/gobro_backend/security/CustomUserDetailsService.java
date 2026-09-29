package com.gobro_backend.security;

import com.gobro_backend.user.entity.Permission;
import com.gobro_backend.user.entity.Role;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * Loads a {@link User} from the database and adapts it to Spring Security's
 * {@link UserDetails} contract. Users authenticate with their email as the
 * "username". Authorities combine the user's role (ROLE_ETUDIANT,
 * ROLE_FORMATEUR, ROLE_ADMIN) with any fine-grained permissions attached
 * to that role, so both hasRole(...) and hasAuthority(...) checks work.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun utilisateur trouvé avec l'email : " + email));

        // Fully-qualified on purpose: "User" here refers to the domain entity
        // imported above, so Spring Security's own User class (used only as
        // a UserDetails factory) must be referenced by its full name.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(buildAuthorities(user))
                .accountExpired(false)
                .accountLocked(!user.isEnabled())
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        Role role = user.getRole();

        Stream<GrantedAuthority> roleAuthority = Stream.of(
                new SimpleGrantedAuthority("ROLE_" + role.getName())
        );

        Stream<GrantedAuthority> permissionAuthorities = role.getPermissions() == null
                ? Stream.empty()
                : role.getPermissions().stream()
                .map(Permission::getName)
                .map(SimpleGrantedAuthority::new);

        return Stream.concat(roleAuthority, permissionAuthorities).toList();
    }
}