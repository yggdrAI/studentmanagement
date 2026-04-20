package com.sms.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.sms.model.User;
import com.sms.repository.UserRepository;

@Service
public class IdentityLookupService {

    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d{10,}$");

    private final UserRepository userRepository;

    public IdentityLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByLoginIdentifier(String identifier) {
        String value = normalizeIdentifier(identifier);
        if (value.isBlank()) {
            return Optional.empty();
        }

        if (isEmail(value)) {
            return userRepository.findByEmailIgnoreCase(value);
        }

        String phoneDigits = digitsOnly(value);
        if (isPhone(value, phoneDigits)) {
            return userRepository.findByPhone(value)
                    .or(() -> userRepository.findByNormalizedPhone(phoneDigits));
        }

        return userRepository.findByUsernameIgnoreCase(value)
                .or(() -> userRepository.findByUsernameOrEmailIgnoreCase(value));
    }

    public Optional<User> findByEmailOrPhone(String emailOrPhone) {
        String value = normalizeIdentifier(emailOrPhone);
        if (value.isBlank()) {
            return Optional.empty();
        }

        if (isEmail(value)) {
            Optional<User> byEmail = userRepository.findByEmailIgnoreCase(value);
            if (byEmail.isPresent()) {
                return byEmail;
            }

            String localPart = emailLocalPart(value);
            if (!localPart.isBlank()) {
                return userRepository.findByUsernameIgnoreCase(localPart)
                        .or(() -> userRepository.findByUsernameOrEmailIgnoreCase(localPart));
            }

            return Optional.empty();
        }

        String phoneDigits = digitsOnly(value);
        if (isPhone(value, phoneDigits)) {
            return userRepository.findByPhone(value)
                    .or(() -> userRepository.findByNormalizedPhone(phoneDigits));
        }

        return Optional.empty();
    }

    public static String normalizeIdentifier(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static boolean isEmail(String value) {
        return value != null && value.contains("@");
    }

    public static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    public static boolean isPhone(String value, String digits) {
        String candidate = digits == null ? "" : digits;
        if (DIGITS_ONLY.matcher(value == null ? "" : value).matches()) {
            return true;
        }
        return candidate.length() >= 10;
    }

    public static String emailLocalPart(String email) {
        if (email == null) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "";
        }
        return email.substring(0, atIndex).trim();
    }
}
