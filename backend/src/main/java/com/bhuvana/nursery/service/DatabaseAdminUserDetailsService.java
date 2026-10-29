package com.bhuvana.nursery.service;

import com.bhuvana.nursery.entity.AdminUser;
import com.bhuvana.nursery.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads the administrator from PostgreSQL instead of an in-memory user
 * manager. This is what allows a password reset to persist across restarts:
 * BCrypt verification always compares against the current database hash.
 */
@Service
public class DatabaseAdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public DatabaseAdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Administrator account not found."
                ));

        return User.withUsername(adminUser.getUsername())
                .password(adminUser.getPasswordHash())
                .roles("ADMIN")
                .disabled(!adminUser.isEnabled())
                .build();
    }
}
