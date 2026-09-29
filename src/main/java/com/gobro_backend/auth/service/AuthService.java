package com.gobro_backend.auth.service;



import com.gobro_backend.auth.dto.AuthResponse;
import com.gobro_backend.auth.dto.LoginRequest;
import com.gobro_backend.auth.dto.RegisterRequest;
import com.gobro_backend.auth.mapper.AuthMapper;
import com.gobro_backend.security.JwtService;
import com.gobro_backend.user.entity.Role;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.RoleRepository;
import com.gobro_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.gobro_backend.auth.entity.RefreshToken;
import com.gobro_backend.auth.repository.RefreshTokenRepository;
import java.time.LocalDateTime;

/**
 * Core authentication service: registration, login (with optional 2FA gate),
 * access token refresh and logout. Password reset and 2FA enrollment live
 * in their own dedicated services (PasswordResetService, TwoFactorAuthService).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final TwoFactorAuthService twoFactorAuthService;
    private final RefreshTokenRepository refreshTokenRepository;

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plus(java.time.Duration.ofMillis(jwtService.getRefreshTokenExpirationMs())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse register(@Valid RegisterRequest request) {
        if (!request.passwordsMatch()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        Role role = roleRepository.findByName(request.requestedRole())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide"));

        User user = authMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User saved = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, saved.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(saved, refreshToken);

        return AuthResponse.tokens(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                authMapper.toUserSummary(saved)
        );
    }

    @Transactional
    public AuthResponse login(@Valid LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte désactivé, contactez le support");
        }

        if (user.isTwoFactorEnabled()) {
            if (request.twoFactorCode() == null || request.twoFactorCode().isBlank()) {
                return AuthResponse.twoFactorRequired(authMapper.toUserSummary(user));
            }
            if (!twoFactorAuthService.verifyCode(user, request.twoFactorCode())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code de vérification invalide");
            }
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        return AuthResponse.tokens(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                authMapper.toUserSummary(user)
        );
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement invalide");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement introuvable ou révoqué"));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement expiré ou révoqué");
        }

        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement expiré ou invalide");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId());

        return AuthResponse.tokens(
                newAccessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                authMapper.toUserSummary(user)
        );
    }

    /**
     * Invalidate all refresh tokens for the user identified by the access token.
     */
    @Transactional
    public void logout(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                String email = jwtService.extractUsername(accessToken);
                userRepository.findByEmail(email).ifPresent(user -> {
                    refreshTokenRepository.deleteAllByUser(user);
                });
            } catch (Exception e) {
                // Access token might be expired or malformed, ignore to let client logout successfully
            }
        }
    }
}