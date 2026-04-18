package com.sms.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * In-memory rate limiter for sensitive endpoints.
 * Keys are client IP + path, using a fixed one-minute window.
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final int DEFAULT_LIMIT = 120;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!isSensitive(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String key = clientIp + "|" + path;
        int limit = resolveLimit(path);
        long now = Instant.now().getEpochSecond();

        WindowCounter counter = counters.compute(key, (k, current) -> {
            if (current == null || now >= current.windowStart + WINDOW_SECONDS) {
                return new WindowCounter(now, 1);
            }
            current.count++;
            return current;
        });

        if (counter != null && counter.count > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSensitive(String path) {
        return "/api/auth/login".equals(path)
            || "/api/student/attendance/mark".equals(path)
            || "/api/student/attendance/verify-qr".equals(path)
            || "/api/student/attendance/register-face".equals(path)
            || "/api/admin/attendance/override".equals(path);
    }

    private int resolveLimit(String path) {
        if ("/api/auth/login".equals(path)) {
            return 20;
        }
        if (path.startsWith("/api/student/attendance/")) {
            return 40;
        }
        if ("/api/admin/attendance/override".equals(path)) {
            return 30;
        }
        return DEFAULT_LIMIT;
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

    private static final class WindowCounter {
        private final long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
