package com.sms.config;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sms.model.AuditLog;
import com.sms.repository.AuditLogRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Persists API audit events with actor, action, status, tenant and origin details.
 */
@Component
public class AdminActionAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminActionAuditFilter.class);
    private static final Set<String> TRACKED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final AuditLogRepository auditLogRepository;

    public AdminActionAuditFilter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || !TRACKED_METHODS.contains(request.getMethod())) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return;
        }

        String ipAddress = extractClientIp(request);
        Long tenantId = extractTenantId(request);
        String action = request.getMethod() + " " + path;

        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(auth.getName());
        auditLog.setAction(action);
        auditLog.setEndpoint(path);
        auditLog.setHttpMethod(request.getMethod());
        auditLog.setStatusCode(response.getStatus());
        auditLog.setTenantId(tenantId);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(trimToMax(request.getHeader("User-Agent"), 500));
        auditLogRepository.save(auditLog);

        log.info("API_AUDIT actor={} method={} path={} status={} tenant={} ip={}",
            auth.getName(),
            request.getMethod(),
            path,
            response.getStatus(),
            tenantId,
            ipAddress
        );
    }

    private Long extractTenantId(HttpServletRequest request) {
        Object tenant = request.getAttribute("tenantId");
        if (tenant instanceof Number number) {
            return number.longValue();
        }
        if (tenant instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int idx = forwarded.indexOf(',');
            return (idx > 0 ? forwarded.substring(0, idx) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String trimToMax(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
