package com.sms.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String TENANT_CLAIM = "tenant_id";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-millis}")
    private long expirationMillis;

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElse("STUDENT");

        return generateToken(userDetails.getUsername(), role, 1L);
        }

    public String generateToken(String username, String role) {
        return generateToken(username, role, 1L);
    }

    public String generateToken(String username, String role, Long tenantId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put(ROLE_CLAIM, role);
        claims.put(TENANT_CLAIM, tenantId == null ? 1L : tenantId);

        return Jwts.builder()
            .claims(claims)
            .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object claim = extractAllClaims(token).get(ROLE_CLAIM);
        return claim != null ? claim.toString() : null;
    }

    public Long extractTenantId(String token) {
        Object claim = extractAllClaims(token).get(TENANT_CLAIM);
        if (claim == null) {
            return 1L;
        }
        if (claim instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(claim.toString());
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
