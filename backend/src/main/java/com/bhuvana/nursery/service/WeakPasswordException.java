package com.bhuvana.nursery.service;

/** Raised when a proposed password does not satisfy {@link PasswordPolicy}. */
public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
