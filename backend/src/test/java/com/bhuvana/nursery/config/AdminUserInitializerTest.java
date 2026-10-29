package com.bhuvana.nursery.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserInitializerTest {

    private final AdminUserRepository repository = mock(AdminUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    private AdminUserInitializer initializer(String username, String password, String email) {
        return new AdminUserInitializer(repository, passwordEncoder, username, password, email);
    }

    @Test
    @DisplayName("Seeds the first administrator with a BCrypt hash and the reset email")
    void seedsFirstAdministrator() {
        when(repository.count()).thenReturn(0L);
        when(passwordEncoder.encode("SecretPass123")).thenReturn("bcrypt-hash");

        initializer("nurseryadmin", "SecretPass123", "admin@example.com").run(args);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(repository).save(captor.capture());

        AdminUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("nurseryadmin");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Backfills a blank reset email on an existing administrator")
    void backfillsBlankEmail() {
        AdminUser existing = existingAdmin(null);
        when(repository.count()).thenReturn(1L);
        when(repository.findByUsername("nurseryadmin")).thenReturn(Optional.of(existing));

        initializer("nurseryadmin", "", "owner@gmail.com").run(args);

        assertThat(existing.getEmail()).isEqualTo("owner@gmail.com");
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("Never overwrites an email that is already stored")
    void keepsConfiguredEmail() {
        AdminUser existing = existingAdmin("owner@gmail.com");
        when(repository.count()).thenReturn(1L);
        when(repository.findByUsername("nurseryadmin")).thenReturn(Optional.of(existing));

        initializer("nurseryadmin", "", "attacker@gmail.com").run(args);

        assertThat(existing.getEmail()).isEqualTo("owner@gmail.com");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Does nothing when no reset email is configured")
    void ignoresBlankEmail() {
        when(repository.count()).thenReturn(1L);

        initializer("nurseryadmin", "", "").run(args);

        verify(repository, never()).save(any());
        verify(repository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Warns at startup when no administrator has a reset email")
    void warnsWhenNoResetEmailIsStored() {
        AdminUser existing = existingAdmin(null);
        when(repository.count()).thenReturn(1L);
        when(repository.findAll()).thenReturn(List.of(existing));

        Logger logger = (Logger) LoggerFactory.getLogger(AdminUserInitializer.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            initializer("nurseryadmin", "", "").run(args);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("ADMIN_EMAIL")
                        .contains("nurseryadmin"));
    }

    @Test
    @DisplayName("Fails clearly when there is no administrator and no seed credentials")
    void requiresSeedCredentials() {
        when(repository.count()).thenReturn(0L);

        assertThatThrownBy(() -> initializer("", "", "").run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME");
    }

    private AdminUser existingAdmin(String email) {
        AdminUser adminUser = new AdminUser();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("nurseryadmin");
        adminUser.setPasswordHash("existing-hash");
        adminUser.setEmail(email);
        adminUser.setEnabled(true);
        return adminUser;
    }
}
