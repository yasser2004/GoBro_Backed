package com.gobro_backend.auth.dto;

/**
 * Response returned by /login, /register and /refresh.
 *
 * When requiresTwoFactor is true, accessToken/refreshToken are null:
 * the client must call /login again including the twoFactorCode.
 */

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean requiresTwoFactor,
        UserSummary user
) {
    public static AuthResponse tokens(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, false, user);
    }

    public static AuthResponse twoFactorRequired(UserSummary user) {
        return new AuthResponse(null, null, null, 0, true, user);
    }

    /**
     * Minimal, non-sensitive user projection embedded in the auth response
     * (no password, no 2FA secret).
     */
    public record UserSummary(
            Long id,
            String fullName,
            String email,
            String role,
            int xp,
            int level
    ) {
    }
}