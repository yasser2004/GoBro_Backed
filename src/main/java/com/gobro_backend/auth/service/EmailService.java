package com.gobro_backend.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendPasswordResetEmail(String email, String fullName, String resetLink) {
        log.info("Sending password reset email to {} ({}): {}", fullName, email, resetLink);
        // Implementation hook: Integrate with JavaMailSender or email service provider (SendGrid, Mailgun, AWS SES, etc.)
    }

    public void sendPasswordChangedConfirmation(String email, String fullName) {
        log.info("Sending password change confirmation email to {} ({})", fullName, email);
        // Implementation hook: Integrate with JavaMailSender or email service provider
    }
}
