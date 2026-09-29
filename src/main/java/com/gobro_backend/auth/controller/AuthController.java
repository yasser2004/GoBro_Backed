package com.gobro_backend.auth.controller;



import com.gobro_backend.auth.dto.AuthResponse;
import com.gobro_backend.auth.dto.LoginRequest;
import com.gobro_backend.auth.dto.RegisterRequest;
import com.gobro_backend.auth.dto.ResetPasswordRequest;
import com.gobro_backend.auth.service.AuthService;
import com.gobro_backend.auth.service.PasswordResetService;
import com.gobro_backend.auth.service.TwoFactorAuthService;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication REST API: registration, login (with optional 2FA gate),
 * token refresh, logout, "mot de passe oublié" flow and 2FA enrollment.
 * All endpoints here are public (see SecurityConfig PUBLIC_ENDPOINTS),
 * except the 2FA enrollment/disable endpoints which require an authenticated user.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription, connexion, rafraîchissement de token, mot de passe oublié et 2FA")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "Créer un compte étudiant ou formateur")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter (retourne requiresTwoFactor=true si le compte a activé la 2FA)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Obtenir un nouvel access token à partir d'un refresh token valide")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (invalidation côté client des tokens)")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Envoyer un email de réinitialisation de mot de passe")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request.email());
        // Always 200/no-content, whether or not the email exists, to avoid account enumeration.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe via le token reçu par email")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Démarrer l'activation de la 2FA : retourne le secret + l'URI QR code à scanner")
    public ResponseEntity<TwoFactorSetupResponse> enableTwoFactor(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        String secret = twoFactorAuthService.generateSecret(user);
        String provisioningUri = twoFactorAuthService.buildProvisioningUri(user, secret);
        return ResponseEntity.ok(new TwoFactorSetupResponse(secret, provisioningUri));
    }

    @PostMapping("/2fa/confirm")
    @Operation(summary = "Confirmer l'activation de la 2FA avec le premier code généré par l'application")
    public ResponseEntity<Void> confirmTwoFactor(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody TwoFactorCodeRequest request
    ) {
        User user = currentUser(principal);
        twoFactorAuthService.confirmEnrollment(user, request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Désactiver la 2FA sur le compte courant")
    public ResponseEntity<Void> disableTwoFactor(@AuthenticationPrincipal UserDetails principal) {
        User user = currentUser(principal);
        twoFactorAuthService.disableTwoFactor(user);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    // --- small request/response records local to this controller ---

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record TwoFactorCodeRequest(@NotBlank String code) {
    }

    public record TwoFactorSetupResponse(String secret, String provisioningUri) {
    }
}