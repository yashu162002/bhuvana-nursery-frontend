package com.bhuvana.nursery.repository;

import com.bhuvana.nursery.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Removes every still-unused token of a user. Called when a new link is
     * requested (so only the newest link works) and after a successful reset.
     */
    @Modifying
    @Query("""
            delete from PasswordResetToken t
            where t.adminUserId = :userId and t.usedAt is null
            """)
    int deleteUnusedTokensFor(@Param("userId") UUID userId);
}
