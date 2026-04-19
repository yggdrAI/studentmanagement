package com.sms.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    public void validateOrThrow(String password) {
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException("Password must be at least 10 characters long");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must include at least one uppercase letter");
        }
        if (!LOWERCASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must include at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must include at least one digit");
        }
        if (!SYMBOL.matcher(password).find()) {
            throw new IllegalArgumentException("Password must include at least one symbol");
        }
    }
}
