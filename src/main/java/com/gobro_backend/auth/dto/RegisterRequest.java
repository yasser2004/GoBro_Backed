package com.gobro_backend.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/auth/register.
 * requestedRole is restricted by AuthService to ETUDIANT or FORMATEUR;
 * ADMIN accounts can never be self-registered through this endpoint.
 */
public record RegisterRequest(

        @NotBlank(message = "Le nom complet est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String fullName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir une majuscule, une minuscule et un chiffre"
        )
        String password,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword,

        @NotBlank(message = "Le rôle est obligatoire")
        @Pattern(regexp = "ETUDIANT|FORMATEUR", message = "Rôle invalide")
        String requestedRole,

        @AssertTrue(message = "Vous devez accepter les conditions d'utilisation")
        Boolean acceptTerms
) {
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}