package com.gobro_backend.auth.service;


import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * TOTP-based two-factor authentication (RFC 6238), compatible with Google
 * Authenticator / Authy / Microsoft Authenticator.
 *
 * Enrollment flow:
 *  1. generateSecret(user)      -> raw secret persisted (not yet active)
 *  2. buildProvisioningUri(...) -> otpauth:// URI to render as a QR code
 *  3. confirmEnrollment(...)    -> user submits a first valid code, 2FA becomes active
 *
 * Login flow:
 *  - verifyCode(user, code) is called from AuthService#login once the user
 *    submits their 6-digit code.
 *
 * Requires the "dev.samstevens:totp" dependency.
 */
@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private final UserRepository userRepository;

    @Value("${app.name:GoBro Platform}")
    private String issuerName;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());

    /**
     * Generates and persists (inactive) a new TOTP secret for the user.
     * The user must confirm with a valid code before twoFactorEnabled flips to true.
     */
    @Transactional
    public String generateSecret(User user) {
        String secret = secretGenerator.generate();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false);
        userRepository.save(user);
        return secret;
    }

    /**
     * Builds the otpauth:// provisioning URI to encode as a QR code on the frontend.
     */
    public String buildProvisioningUri(User user, String secret) {
        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(issuerName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    /**
     * Confirms 2FA enrollment: verifies the first code typed by the user
     * against the freshly generated secret, then activates 2FA on the account.
     */
    @Transactional
    public void confirmEnrollment(User user, String code) {
        if (user.getTwoFactorSecret() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune procédure d'activation 2FA en cours");
        }
        if (!codeVerifier.isValidCode(user.getTwoFactorSecret(), code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code de vérification invalide");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    /**
     * Verifies a 6-digit TOTP code against the user's active secret.
     * Used both at login time and to gate sensitive actions.
     */
    public boolean verifyCode(User user, String code) {
        if (user.getTwoFactorSecret() == null || !user.isTwoFactorEnabled()) {
            return false;
        }
        return codeVerifier.isValidCode(user.getTwoFactorSecret(), code);
    }

    @Transactional
    public void disableTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }
}