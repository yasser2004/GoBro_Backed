package com.gobro_backend.user.controller;


import com.gobro_backend.user.dto.UpdateUserRequest;
import com.gobro_backend.user.dto.UserResponse;
import com.gobro_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * User profile endpoints (self-service, any authenticated role) and
 * administration endpoints (ADMIN only — see cahier des charges §13,
 * "Gestion des utilisateurs").
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion du profil et administration des comptes")
public class UserController {

    private final UserService userService;

    // ---------- Self-service (Étudiant / Formateur / Admin) ----------

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getByEmail(principal.getUsername()));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil de l'utilisateur connecté")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUsername(), request));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Changer le mot de passe de l'utilisateur connecté")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getUsername(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    // ---------- Admin (cahier des charges §13 - Administration) ----------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister / rechercher les utilisateurs (admin)")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.list(keyword, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Récupérer un utilisateur par id (admin)")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer / désactiver un compte (admin)")
    public ResponseEntity<UserResponse> setStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Changer le rôle d'un utilisateur (admin)")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        return ResponseEntity.ok(userService.assignRole(id, request.role()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un compte utilisateur (admin)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- small request records local to this controller ---

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword
    ) {
    }

    public record AssignRoleRequest(@NotBlank String role) {
    }
}