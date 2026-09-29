package com.gobro_backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA configuration: enables repositories scanning, auditing (createdBy/updatedBy
 * on entities via @CreatedBy/@LastModifiedBy) and declarative transactions.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.gobro_backend")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {

    /**
     * Resolves the current authenticated username for JPA auditing fields.
     * Falls back to "system" for scheduled jobs / unauthenticated writes.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}