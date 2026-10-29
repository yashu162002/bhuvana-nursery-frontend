package com.bhuvana.nursery.service;

/** Raised when a reset token is unknown, already used, or expired. */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("This reset link is invalid or has expired.");
    }
}
