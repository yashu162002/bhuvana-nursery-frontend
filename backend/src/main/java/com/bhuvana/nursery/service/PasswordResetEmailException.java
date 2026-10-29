package com.bhuvana.nursery.service;

/** Thrown when a password reset email could not be dispatched. */
public class PasswordResetEmailException extends RuntimeException {

    public PasswordResetEmailException(String message) {
        super(message);
    }

    public PasswordResetEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
