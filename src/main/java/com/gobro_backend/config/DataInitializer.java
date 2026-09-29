package com.gobro_backend.config;

import com.gobro_backend.user.entity.Role;
import com.gobro_backend.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            String[] roles = {"ETUDIANT", "FORMATEUR", "ADMIN"};
            for (String roleName : roles) {
                roleRepository.findByName(roleName).orElseGet(() -> {
                    Role role = new Role(roleName);
                    role.setDescription(switch (roleName) {
                        case "ETUDIANT" -> "Student role";
                        case "FORMATEUR" -> "Instructor role";
                        case "ADMIN" -> "Administrator role";
                        default -> "";
                    });
                    return roleRepository.save(role);
                });
            }
        };
    }
}
