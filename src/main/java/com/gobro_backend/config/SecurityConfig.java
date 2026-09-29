package com.gobro_backend.config;

import com.gobro_backend.security.JwtAuthenticationFilter;
import com.gobro_backend.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Core Spring Security configuration.
 *
 * Stateless JWT authentication for the REST API.
 * OAuth2 login delegates to OAuth2AuthenticationSuccessHandler.
 *
 * Roles:
 * - ETUDIANT
 * - FORMATEUR
 * - ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/courses/public/**",
            "/api/v1/categories/**",
            "/api/v1/instructors/public/**",
            "/api/v1/certificates/verify/**",
            "/api/v1/reviews/course/**",

            // OAuth2
            "/oauth2/**",
            "/login/**",

            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Actuator
            "/actuator/health",

            // H2 Console
            "/h2-console/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )

                .csrf(csrf -> csrf.disable())

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(PUBLIC_ENDPOINTS)
                        .permitAll()

                        // Admin
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        // Instructor + Admin
                        .requestMatchers(
                                "/api/v1/instructor/**",
                                "/api/v1/courses/manage/**"
                        )
                        .hasAnyRole("FORMATEUR", "ADMIN")

                        // Student + Admin
                        .requestMatchers("/api/v1/students/**", "/api/v1/enrollments/**")
                        .hasAnyRole("ETUDIANT", "ADMIN")

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                // OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // Authentication provider
                .authenticationProvider(authenticationProvider())

                // JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // Exception handling
                .exceptionHandling(handling -> handling

                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                401,
                                                "Unauthorized: " + authException.getMessage()
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.sendError(
                                                403,
                                                "Forbidden: " + accessDeniedException.getMessage()
                                        )
                        )
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(12);
    }
}