package com.bhuvana.nursery.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small in-memory fixed-window rate limiter. Sufficient for a single-node
 * deployment with one administrator; replace with a shared store (or Spring
 * Cloud Gateway rate limiting) if the API is ever scaled horizontally.
 */
@Component
public class InMemoryRateLimiter {

    private static final int CLEANUP_THRESHOLD = 512;

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Consumes one unit for {@code key}. Returns {@code false} when the limit
     * for the current window is already exhausted.
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = clock.millis();
        long windowMillis = window.toMillis();

        if (windows.size() > CLEANUP_THRESHOLD) {
            removeExpired(now, windowMillis);
        }

        Window current = windows.compute(key, (k, existing) ->
                (existing == null || existing.isExpired(now, windowMillis))
                        ? new Window(now)
                        : existing
        );

        return current.tryConsume(limit);
    }

    /** Test hook: drops all counters. */
    public void reset() {
        windows.clear();
    }

    private void removeExpired(long now, long windowMillis) {
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Window> entry = iterator.next();
            if (entry.getValue().isExpired(now, windowMillis)) {
                iterator.remove();
            }
        }
    }

    private static final class Window {

        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(long now, long windowMillis) {
            return now - startedAt >= windowMillis;
        }

        private boolean tryConsume(int limit) {
            return count.incrementAndGet() <= limit;
        }
    }
}
