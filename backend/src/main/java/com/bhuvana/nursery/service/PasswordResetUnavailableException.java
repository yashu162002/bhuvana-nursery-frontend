package com.bhuvana.nursery.service;

/** Raised when password reset emails cannot be sent in this environment. */
public class PasswordResetUnavailableException extends RuntimeException {

    public PasswordResetUnavailableException(String message) {
        super(message);
    }
}
