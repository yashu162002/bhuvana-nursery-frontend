package com.bhuvana.nursery.service;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.entity.PasswordResetToken;
import com.bhuvana.nursery.repository.AdminUserRepository;
import com.bhuvana.nursery.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues single-use, short-lived password reset tokens and redeems them.
 *
 * <ul>
 *   <li>Tokens are 256-bit values from {@link SecureRandom}; only their
 *       SHA-256 hash is persisted.</li>
 *   <li>Every response to the public API is identical whether or not the
 *       submitted email exists, so accounts cannot be enumerated.</li>
 *   <li>Requesting a new link invalidates every earlier unused link.</li>
 *   <li>A redeemed token can never be used again.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdminUserRepository adminUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailSender emailSender;
    private final InMemoryRateLimiter rateLimiter;
    private final AdminSessionRegistry sessionRegistry;
    private final Clock clock;

    private final Duration tokenTtl;
    private final int requestsPerIp;
    private final int requestsPerEmail;
    private final Duration rateWindow;

    public PasswordResetService(
            AdminUserRepository adminUserRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetEmailSender emailSender,
            InMemoryRateLimiter rateLimiter,
            AdminSessionRegistry sessionRegistry,
            Clock clock,
            @Value("${app.password-reset.ttl-minutes:20}") long ttlMinutes,
            @Value("${app.password-reset.rate-limit.requests-per-ip:10}") int requestsPerIp,
            @Value("${app.password-reset.rate-limit.requests-per-email:3}") int requestsPerEmail,
            @Value("${app.password-reset.rate-limit.window-minutes:60}") long windowMinutes
    ) {
        this.adminUserRepository = adminUserRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.rateLimiter = rateLimiter;
        this.sessionRegistry = sessionRegistry;
        this.clock = clock;
        this.tokenTtl = Duration.ofMinutes(ttlMinutes);
        this.requestsPerIp = requestsPerIp;
        this.requestsPerEmail = requestsPerEmail;
        this.rateWindow = Duration.ofMinutes(windowMinutes);
    }

    /**
     * Accepts a reset request for {@code email}. Returns normally whether or
     * not an account matches, so callers can answer generically; throws only
     * for rate limiting or missing SMTP configuration.
     */
    @Transactional
    public void requestReset(String email, String clientIp) {
        String normalizedEmail = email == null ? "" : email.trim();

        if (!emailSender.isConfigured()) {
            throw new PasswordResetUnavailableException(
                    "Password reset emails are not configured."
            );
        }

        if (!rateLimiter.tryAcquire("ip:" + clientIp, requestsPerIp, rateWindow) ||
                !rateLimiter.tryAcquire("email:" + normalizedEmail.toLowerCase(),
                        requestsPerEmail, rateWindow)) {
            throw new PasswordResetRateLimitedException();
        }

        AdminUser adminUser = adminUserRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        if (adminUser == null || !adminUser.isEnabled()) {
            // Identical outcome to a successful request: no token, no email.
            return;
        }

        // Only the most recently requested link stays valid.
        tokenRepository.deleteUnusedTokensFor(adminUser.getId());

        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setAdminUserId(adminUser.getId());
        token.setTokenHash(sha256Hex(rawToken));
        token.setExpiresAt(clock.instant().plus(tokenTtl));
        tokenRepository.save(token);

        try {
            emailSender.sendResetLink(
                    adminUser.getEmail(),
                    adminUser.getUsername(),
                    rawToken,
                    tokenTtl
            );
            // Token-free audit line so an operator can confirm delivery attempts
            // in the server log (the generic API response proves nothing).
            log.info("Password reset email sent for administrator '{}'.",
                    adminUser.getUsername());
        } catch (PasswordResetEmailException ex) {
            // No token or link in the log; the API still answers generically so
            // a send failure cannot be used to probe for accounts.
            log.error("Password reset email could not be sent: {}", ex.getMessage());
        }
    }

    /**
     * Redeems a reset token and persists the new BCrypt hash. The change
     * survives restarts because login always verifies against this row.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String policyError = PasswordPolicy.validate(newPassword);
        if (policyError != null) {
            throw new WeakPasswordException(policyError);
        }

        if (!StringUtils.hasText(rawToken) || rawToken.length() > 100) {
            throw new InvalidResetTokenException();
        }

        PasswordResetToken token = tokenRepository
                .findByTokenHash(sha256Hex(rawToken.trim()))
                .orElseThrow(InvalidResetTokenException::new);

        Instant now = clock.instant();

        if (token.isUsed() || token.isExpired(now)) {
            throw new InvalidResetTokenException();
        }

        AdminUser adminUser = adminUserRepository
                .findById(token.getAdminUserId())
                .orElseThrow(InvalidResetTokenException::new);

        if (!adminUser.isEnabled()) {
            throw new InvalidResetTokenException();
        }

        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUser.setPasswordChangedAt(now);
        adminUserRepository.save(adminUser);

        token.setUsedAt(now);
        tokenRepository.save(token);

        // Any other outstanding link for this account dies with it.
        tokenRepository.deleteUnusedTokensFor(adminUser.getId());

        // Existing browser sessions must not keep administrative access after
        // the credentials were rotated.
        sessionRegistry.revokeAll(adminUser.getUsername());

        log.info("Password reset completed for administrator account '{}'.",
                adminUser.getUsername());
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of a value as lowercase hex; public so tests can assert on hashes. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
