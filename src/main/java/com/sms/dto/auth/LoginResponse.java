package com.sms.dto.auth;

public class LoginResponse {

    private final String token;
    private final String role;
    private final long expiresAt;

    public LoginResponse(String token, String role, long expiresAt) {
        this.token = token;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
