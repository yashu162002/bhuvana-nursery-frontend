package com.bhuvana.nursery.service;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks live administrator HTTP sessions so they can be revoked when the
 * password changes. A password reset that left old sessions valid would let a
 * stolen browser session keep administrative access after the credentials were
 * rotated.
 *
 * <p>In-memory, like {@link InMemoryRateLimiter}: correct for the current
 * single-node deployment. A horizontally scaled deployment should replace this
 * with Spring Session / a shared session store.</p>
 */
@Component
public class AdminSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionRegistry.class);

    private final ConcurrentHashMap<String, Set<HttpSession>> sessionsByUsername =
            new ConcurrentHashMap<>();

    /** Records a session created by a successful login. */
    public void register(String username, HttpSession session) {
        if (username == null || session == null) {
            return;
        }

        sessionsByUsername
                .computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /** Removes a session that logged out normally. */
    public void unregister(String username, HttpSession session) {
        if (username == null || session == null) {
            return;
        }

        Set<HttpSession> userSessions = sessionsByUsername.get(username);
        if (userSessions == null) {
            return;
        }

        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessionsByUsername.remove(username, userSessions);
        }
    }

    /**
     * Invalidates every session belonging to {@code username}. Called after a
     * successful password reset so the old credentials and their sessions stop
     * working immediately.
     *
     * @return the number of sessions that were still active and got revoked
     */
    public int revokeAll(String username) {
        if (username == null) {
            return 0;
        }

        Set<HttpSession> userSessions = sessionsByUsername.remove(username);
        if (userSessions == null) {
            return 0;
        }

        int revoked = 0;

        for (HttpSession session : userSessions) {
            try {
                session.invalidate();
                revoked += 1;
            } catch (IllegalStateException alreadyExpired) {
                // The container expired it first; nothing left to revoke.
            }
        }

        if (revoked > 0) {
            // Username only: never log credentials or tokens.
            log.info(
                    "Revoked {} active session(s) for administrator '{}' after a password change.",
                    revoked,
                    username
            );
        }

        return revoked;
    }

    /** Test hook: number of sessions currently tracked for an account. */
    public int activeSessionCount(String username) {
        Set<HttpSession> userSessions = sessionsByUsername.get(username);
        return userSessions == null ? 0 : userSessions.size();
    }
}
