package com.sms.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sms.model.User;
import com.sms.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FirstLoginEnforcementFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public FirstLoginEnforcementFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/student/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("/api/student/profile/password".equals(path) || "/api/student/profile/demographics".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
        if (!isStudent) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = extractUsername(authentication);
        if (username == null || username.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && Boolean.TRUE.equals(user.getIsFirstLogin())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Please change your default password before accessing student APIs\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String username) {
            return username;
        }

        return authentication.getName();
    }
}
