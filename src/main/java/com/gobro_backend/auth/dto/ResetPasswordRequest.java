package com.gobro_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/auth/reset-password (final step of the "mot de
 * passe oublié" flow). The token is the one emailed to the user by
 * PasswordResetService#initiatePasswordReset.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "Le token de réinitialisation est obligatoire")
        String token,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir une majuscule, une minuscule et un chiffre"
        )
        String newPassword,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword
) {
    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}