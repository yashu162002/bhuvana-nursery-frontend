package com.bhuvana.nursery.service;

/** Raised when a client exceeds the password reset request budget. */
public class PasswordResetRateLimitedException extends RuntimeException {

    public PasswordResetRateLimitedException() {
        super("Too many password reset requests.");
    }
}
