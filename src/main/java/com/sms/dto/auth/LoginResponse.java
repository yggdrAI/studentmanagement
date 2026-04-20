package com.sms.dto.auth;

import java.util.List;

public class LoginResponse {

    private final String token;
    private final String role;
    private final Long tenantId;
    private final List<String> permissions;
    private final long expiresAt;
    private final boolean firstLoginRequired;

    public LoginResponse(String token,
                         String role,
                         Long tenantId,
                         List<String> permissions,
                         long expiresAt,
                         boolean firstLoginRequired) {
        this.token = token;
        this.role = role;
        this.tenantId = tenantId;
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
        this.expiresAt = expiresAt;
        this.firstLoginRequired = firstLoginRequired;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isFirstLoginRequired() {
        return firstLoginRequired;
    }
}
