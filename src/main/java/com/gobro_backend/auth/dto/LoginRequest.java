package com.gobro_backend.auth.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/v1/auth/login.
 * twoFactorCode is only required when the account has 2FA enabled;
 * in that case a first call without the code returns
 * AuthResponse.requiresTwoFactor = true instead of tokens.
 */
public record LoginRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password,

        @Size(min = 6, max = 6, message = "Le code doit contenir 6 chiffres")
        String twoFactorCode
) {
}