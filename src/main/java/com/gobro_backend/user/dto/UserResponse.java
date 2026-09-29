package com.gobro_backend.user.dto;


import java.time.LocalDateTime;


public record UserResponse(
        Long id,
        String fullName,
        String email,
        String avatarUrl,
        String bio,
        String phoneNumber,
        String role,
        boolean enabled,
        boolean twoFactorEnabled,
        int xp,
        int level,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}