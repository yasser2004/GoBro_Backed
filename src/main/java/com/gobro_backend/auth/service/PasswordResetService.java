package com.gobro_backend.auth.service;



import com.gobro_backend.auth.dto.ResetPasswordRequest;
import com.gobro_backend.auth.entity.PasswordResetToken;
import com.gobro_backend.auth.repository.PasswordResetTokenRepository;

import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "Mot de passe oublié" flow.
 *
 * 1. initiatePasswordReset: generates a single-use, time-limited token and
 *    emails a reset link to the user (if the email exists — the response
 *    is always generic to avoid leaking account existence).
 * 2. resetPassword: validates the token and updates the password.
 *
 * Requires a PasswordResetToken entity/repository under
 * com.gobro_backend.auth.entity / .repository (id, token, user, expiryDate, used).
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.password-reset.token-expiration-minutes:30}")
    private long tokenExpirationMinutes;

    @Value("${app.password-reset.frontend-url:http://localhost:4200/reset-password}")
    private String resetPasswordFrontendUrl;

    @Transactional
    public void initiatePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUser(user);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            String resetLink = resetPasswordFrontendUrl + "?token=" + resetToken.getToken();
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        });
        // Intentionally silent if the email is unknown: same response either way.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.passwordsMatch()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalide"));

        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de réinitialisation a déjà été utilisé");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien de réinitialisation a expiré");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordChangedConfirmation(user.getEmail(), user.getFullName());
    }
}