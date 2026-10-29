package com.bhuvana.nursery.config;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Creates the first administrator from the environment variables that used to
 * be the only source of the credential.
 *
 * <p>The account is only seeded while the table is empty. Once it exists, the
 * database hash wins and is never overwritten on restart, so a password set
 * through the reset flow stays in effect across restarts and deployments.</p>
 */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    private final String username;
    private final String password;
    private final String email;

    public AdminUserInitializer(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:}") String username,
            @Value("${ADMIN_PASSWORD:}") String password,
            @Value("${ADMIN_EMAIL:}") String email
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            // An administrator already exists; the database is authoritative
            // for the password. The reset email can still be filled in from the
            // environment while it is blank, so enabling Gmail later does not
            // require manual SQL.
            syncResetEmail();
            return;
        }

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "No administrator exists in the database and ADMIN_USERNAME / " +
                    "ADMIN_PASSWORD are not set. Set them once (for example in " +
                    "backend/.env) to create the initial administrator account."
            );
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username.trim());
        adminUser.setPasswordHash(passwordEncoder.encode(password));
        adminUser.setEmail(StringUtils.hasText(email) ? email.trim() : null);
        adminUser.setPasswordChangedAt(Instant.now());
        adminUser.setEnabled(true);

        adminUserRepository.save(adminUser);

        // Username only: never log the password or its hash.
        log.info("Seeded initial administrator account '{}'.", adminUser.getUsername());
    }

    /**
     * Backfills the password-reset address from ADMIN_EMAIL when the stored
     * value is blank. An already configured address is never overwritten, so a
     * changed environment variable cannot silently redirect reset emails.
     */
    private void syncResetEmail() {
        if (!StringUtils.hasText(email)) {
            warnWhenNoResetEmailIsStored();
            return;
        }

        String configuredEmail = email.trim();

        var target = StringUtils.hasText(username)
                ? adminUserRepository.findByUsername(username.trim())
                : adminUserRepository.findAll().stream().findFirst();

        target.ifPresent(adminUser -> {
            if (!StringUtils.hasText(adminUser.getEmail())) {
                adminUser.setEmail(configuredEmail);
                adminUserRepository.save(adminUser);
                log.info(
                        "Set the password-reset email for administrator '{}' from ADMIN_EMAIL.",
                        adminUser.getUsername()
                );
            } else if (!adminUser.getEmail().equalsIgnoreCase(configuredEmail)) {
                log.warn(
                        "ADMIN_EMAIL differs from the stored reset email for '{}'; " +
                        "keeping the database value. Update admin_users.email to change it.",
                        adminUser.getUsername()
                );
            }
        });
    }

    /**
     * The forgot-password endpoint answers generically whether or not an
     * account matches, so a missing reset address previously failed without
     * any server-side hint. Warn once at startup instead.
     */
    private void warnWhenNoResetEmailIsStored() {
        var withoutEmail = adminUserRepository.findAll().stream()
                .filter(adminUser -> !StringUtils.hasText(adminUser.getEmail()))
                .map(AdminUser::getUsername)
                .toList();

        if (!withoutEmail.isEmpty()) {
            log.warn(
                    "Password reset email is disabled for administrator(s) {}: " +
                    "no reset email is stored and ADMIN_EMAIL is empty. Set " +
                    "ADMIN_EMAIL to the administrator's inbox and restart.",
                    withoutEmail
            );
        }
    }
}
