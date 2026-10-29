package com.bhuvana.nursery.service;

import java.time.Duration;

/**
 * Delivers a password reset link to the administrator's mailbox. Implementations
 * must never log the raw token or the reset link.
 */
public interface PasswordResetEmailSender {

    /** {@code false} when SMTP has not been configured in this environment. */
    boolean isConfigured();

    void sendResetLink(String toEmail, String username, String rawToken, Duration ttl);
}
