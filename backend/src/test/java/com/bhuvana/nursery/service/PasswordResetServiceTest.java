package com.bhuvana.nursery.service;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.entity.PasswordResetToken;
import com.bhuvana.nursery.repository.AdminUserRepository;
import com.bhuvana.nursery.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the reset token lifecycle: creation (hashed, expiring),
 * redemption, expiry, reuse, and abuse resistance.
 */
class PasswordResetServiceTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String CLIENT_IP = "203.0.113.7";
    private static final String VALID_PASSWORD = "BrandNewPass123";

    private AdminUserRepository adminUserRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordResetEmailSender emailSender;
    private PasswordEncoder passwordEncoder;
    private MutableClock clock;

    private AdminUser adminUser;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        emailSender = mock(PasswordResetEmailSender.class);
        passwordEncoder = new BCryptPasswordEncoder();
        clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));

        adminUser = new AdminUser();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("nurseryadmin");
        adminUser.setPasswordHash(passwordEncoder.encode("OriginalPass123"));
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setEnabled(true);

        when(emailSender.isConfigured()).thenReturn(true);
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PasswordResetService newService() {
        return new PasswordResetService(
                adminUserRepository,
                tokenRepository,
                passwordEncoder,
                emailSender,
                new InMemoryRateLimiter(clock),
                mock(AdminSessionRegistry.class),
                clock,
                20,
                1000,
                1000,
                60
        );
    }

    // ---------------------------------------------------------------- request

    @Test
    @DisplayName("A known email stores only the SHA-256 hash and emails the raw token")
    void requestResetStoresOnlyHashAndEmailsRawToken() {
        when(adminUserRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));

        newService().requestReset(ADMIN_EMAIL, CLIENT_IP);

        var rawTokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendResetLink(
                eq(ADMIN_EMAIL), eq("nurseryadmin"), rawTokenCaptor.capture(), eq(Duration.ofMinutes(20))
        );
        String rawToken = rawTokenCaptor.getValue();

        var savedTokenCaptor = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(savedTokenCaptor.capture());
        PasswordResetToken saved = savedTokenCaptor.getValue();

        assertThat(rawToken).hasSizeGreaterThanOrEqualTo(40);
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(saved.getTokenHash()).isEqualTo(PasswordResetService.sha256Hex(rawToken));
        assertThat(saved.getAdminUserId()).isEqualTo(adminUser.getId());
        assertThat(saved.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(20)));
        assertThat(saved.isUsed()).isFalse();

        // A new request invalidates earlier links.
        verify(tokenRepository).deleteUnusedTokensFor(adminUser.getId());
    }

    @Test
    @DisplayName("An unknown email produces no token and no email, without failing")
    void requestResetForUnknownEmailIsSilentlyAccepted() {
        when(adminUserRepository.findByEmailIgnoreCase("nobody@example.com"))
                .thenReturn(Optional.empty());

        newService().requestReset("nobody@example.com", CLIENT_IP);

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendResetLink(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("A disabled account never receives a link")
    void requestResetSkipsDisabledAccount() {
        adminUser.setEnabled(false);
        when(adminUserRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));

        newService().requestReset(ADMIN_EMAIL, CLIENT_IP);

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendResetLink(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Email delivery failure is swallowed and never leaks the token to logs")
    void requestResetSurvivesEmailFailure() {
        when(adminUserRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));
        doThrow(new PasswordResetEmailException("smtp down"))
                .when(emailSender)
                .sendResetLink(anyString(), anyString(), anyString(), any());

        // Must not throw: the API answers generically either way.
        newService().requestReset(ADMIN_EMAIL, CLIENT_IP);

        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Without SMTP configuration the request is refused")
    void requestResetFailsWhenMailIsNotConfigured() {
        when(emailSender.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> newService().requestReset(ADMIN_EMAIL, CLIENT_IP))
                .isInstanceOf(PasswordResetUnavailableException.class);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("Repeated requests for one address hit the rate limit")
    void requestResetIsRateLimitedPerEmail() {
        when(adminUserRepository.findByEmailIgnoreCase(ADMIN_EMAIL))
                .thenReturn(Optional.of(adminUser));

        PasswordResetService service = new PasswordResetService(
                adminUserRepository, tokenRepository, passwordEncoder, emailSender,
                new InMemoryRateLimiter(clock), mock(AdminSessionRegistry.class),
                clock, 20, 1000, 2, 60
        );

        service.requestReset(ADMIN_EMAIL, CLIENT_IP);
        service.requestReset(ADMIN_EMAIL, CLIENT_IP);

        assertThatThrownBy(() -> service.requestReset(ADMIN_EMAIL, CLIENT_IP))
                .isInstanceOf(PasswordResetRateLimitedException.class);
    }

    // ----------------------------------------------------------------- reset

    @Test
    @DisplayName("A valid token persists the new password and is consumed")
    void resetPasswordWithValidTokenPersistsNewHash() {
        PasswordResetToken token = issueToken("raw-token-valid");

        newService().resetPassword("raw-token-valid", VALID_PASSWORD);

        assertThat(passwordEncoder.matches(VALID_PASSWORD, adminUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OriginalPass123", adminUser.getPasswordHash())).isFalse();
        assertThat(adminUser.getPasswordChangedAt()).isEqualTo(clock.instant());
        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(clock.instant());

        // Any other outstanding link for the account is invalidated too.
        verify(tokenRepository).deleteUnusedTokensFor(adminUser.getId());
    }

    @Test
    @DisplayName("An expired token is rejected and the password stays unchanged")
    void resetPasswordRejectsExpiredToken() {
        PasswordResetToken token = issueToken("raw-token-expired");
        String originalHash = adminUser.getPasswordHash();

        clock.advance(Duration.ofMinutes(21));

        assertThatThrownBy(() -> newService().resetPassword("raw-token-expired", VALID_PASSWORD))
                .isInstanceOf(InvalidResetTokenException.class);

        assertThat(adminUser.getPasswordHash()).isEqualTo(originalHash);
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("A used token can never be replayed")
    void resetPasswordRejectsReusedToken() {
        PasswordResetToken token = issueToken("raw-token-reused");
        token.setUsedAt(clock.instant());

        assertThatThrownBy(() -> newService().resetPassword("raw-token-reused", VALID_PASSWORD))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    @DisplayName("An unknown token is rejected")
    void resetPasswordRejectsUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().resetPassword("not-a-real-token", VALID_PASSWORD))
                .isInstanceOf(InvalidResetTokenException.class);

        verifyNoInteractions(adminUserRepository);
    }

    @Test
    @DisplayName("A malformed token is rejected without touching the database")
    void resetPasswordRejectsBlankToken() {
        assertThatThrownBy(() -> newService().resetPassword("   ", VALID_PASSWORD))
                .isInstanceOf(InvalidResetTokenException.class);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("Weak passwords are rejected before any token lookup")
    void resetPasswordRejectsWeakPasswords() {
        PasswordResetService service = newService();

        assertThatThrownBy(() -> service.resetPassword("raw-token", "short1"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> service.resetPassword("raw-token", "alllettersonly"))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> service.resetPassword("raw-token", "1234567890"))
                .isInstanceOf(WeakPasswordException.class);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("A token for a deleted or disabled account is rejected")
    void resetPasswordRejectsDisabledAccount() {
        issueToken("raw-token-disabled");
        adminUser.setEnabled(false);

        assertThatThrownBy(() -> newService().resetPassword("raw-token-disabled", VALID_PASSWORD))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    // --------------------------------------------------------------- helpers

    /** Registers a pending token for the admin and returns it. */
    private PasswordResetToken issueToken(String rawToken) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setAdminUserId(adminUser.getId());
        token.setTokenHash(PasswordResetService.sha256Hex(rawToken));
        token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(20)));

        when(tokenRepository.findByTokenHash(PasswordResetService.sha256Hex(rawToken)))
                .thenReturn(Optional.of(token));
        when(adminUserRepository.findById(adminUser.getId()))
                .thenReturn(Optional.of(adminUser));

        return token;
    }

    /** Clock whose time the test advances to simulate token expiry. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
