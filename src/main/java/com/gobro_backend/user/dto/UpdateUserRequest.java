package com.gobro_backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for PUT /api/v1/users/me. All fields are optional (null = unchanged),
 * so the client can send a partial update. Email, password, role and
 * gamification fields are intentionally not editable through this endpoint.
 */
public record UpdateUserRequest(

        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String fullName,

        @Size(max = 1000, message = "La bio ne doit pas dépasser 1000 caractères")
        String bio,

        @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "Numéro de téléphone invalide")
        String phoneNumber,

        @Size(max = 500)
        String avatarUrl
) {
}