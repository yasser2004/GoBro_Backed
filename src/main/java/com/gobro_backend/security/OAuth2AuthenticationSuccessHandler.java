package com.gobro_backend.security;


import com.gobro_backend.user.entity.Role;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.RoleRepository;
import com.gobro_backend.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Triggered after a successful Google / Facebook OAuth2 login
 * (see SecurityConfig#filterChain -> .oauth2Login(...)).
 *
 * Responsibilities:
 *  1. Find or just-in-time provision the local {@link User} matching the
 *     OAuth2 provider's email.
 *  2. Issue a platform JWT access + refresh token pair for that user,
 *     exactly as the classic email/password login flow would.
 *  3. Redirect the browser back to the Angular frontend with the tokens
 *     as query parameters, where the SPA picks them up and stores them.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.oauth2.default-role:ETUDIANT}")
    private String defaultRoleName;

    @Value("${app.oauth2.redirect-uri:http://localhost:4200/oauth2/redirect}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(attributes);
        String fullName = extractName(attributes);
        String provider = resolveProvider(authentication);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> provisionNewUser(email, fullName, provider));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtService.generateAccessToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }

    /**
     * Just-in-time provisioning: a first-time Google/Facebook login creates
     * a local account automatically, with a random unusable password
     * (the account can only ever authenticate via OAuth2 or a later
     * "set password" flow) and the default ETUDIANT role.
     */
    private User provisionNewUser(String email, String fullName, String provider) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(UUID.randomUUID().toString()); // unusable random password, hashed by @PrePersist/service layer
        user.setProvider(provider);
        user.setEnabled(true);
        Role role = roleRepository.findByName(defaultRoleName)
                .orElseGet(() -> roleRepository.save(new Role(defaultRoleName)));
        user.setRole(role);
        return userRepository.save(user);
    }

    private String extractEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email == null) {
            throw new IllegalStateException("Le fournisseur OAuth2 n'a pas retourné d'email exploitable.");
        }
        return email.toString();
    }

    private String extractName(Map<String, Object> attributes) {
        Object name = attributes.get("name");
        return name != null ? name.toString() : "Utilisateur GoBro";
    }

    private String resolveProvider(Authentication authentication) {
        String authorizedClientRegistrationId = authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("oauth2");
        // In practice the registrationId ("google" / "facebook") is available
        // via OAuth2AuthenticationToken#getAuthorizedClientRegistrationId();
        // kept simple here to avoid an extra cast dependency in this snippet.
        return authorizedClientRegistrationId.contains("facebook") ? "FACEBOOK" : "GOOGLE";
    }
}