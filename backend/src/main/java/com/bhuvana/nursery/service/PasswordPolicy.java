package com.bhuvana.nursery.service;

import java.util.regex.Pattern;

/**
 * Password rules shared with the frontend reset page: at least 10 characters,
 * containing at least one letter and one digit.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 200;

    public static final String RULES_MESSAGE =
            "Password must be at least " + MIN_LENGTH +
            " characters and include both a letter and a number.";

    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private PasswordPolicy() {
    }

    /** Returns an error message, or {@code null} when the password is valid. */
    public static String validate(String password) {
        if (password == null || password.length() < MIN_LENGTH ||
                password.length() > MAX_LENGTH) {
            return RULES_MESSAGE;
        }
        if (!LETTER.matcher(password).matches() || !DIGIT.matcher(password).matches()) {
            return RULES_MESSAGE;
        }
        return null;
    }

    public static boolean isValid(String password) {
        return validate(password) == null;
    }
}
