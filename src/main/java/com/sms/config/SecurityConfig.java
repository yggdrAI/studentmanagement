package com.sms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import com.sms.service.CustomUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FirstLoginEnforcementFilter firstLoginEnforcementFilter;
    private final TenantContextFilter tenantContextFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final AdminActionAuditFilter adminActionAuditFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          FirstLoginEnforcementFilter firstLoginEnforcementFilter,
                          TenantContextFilter tenantContextFilter,
                          ApiRateLimitFilter apiRateLimitFilter,
                          AdminActionAuditFilter adminActionAuditFilter,
                          CustomUserDetailsService customUserDetailsService,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.firstLoginEnforcementFilter = firstLoginEnforcementFilter;
        this.tenantContextFilter = tenantContextFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.adminActionAuditFilter = adminActionAuditFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authenticationProvider(authenticationProvider())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/icons/**", "/manifest.json", "/service-worker.js", "/ws/**", "/topic/**", "/app/**", "/student-login").permitAll()
                .requestMatchers("/login", "/forgot-password", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/change-password", "/api/auth/me").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                .requestMatchers("/api/student/**").hasRole("STUDENT")
                .requestMatchers("/api/analytics/**").hasAuthority("VIEW_ANALYTICS")
                .requestMatchers("/api/**").denyAll()
                .requestMatchers("/attendance/scanner").hasRole("STUDENT")
                .requestMatchers("/attendance/teacher").hasRole("TEACHER")
                .requestMatchers("/attendance/reports").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/teacher/**").hasRole("TEACHER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    String loginPortal = request.getParameter("loginPortal");

                    boolean isStudent = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
                    boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                    boolean isTeacher = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_TEACHER".equals(authority.getAuthority()));

                    if ("staff".equalsIgnoreCase(loginPortal) && isStudent && !isAdmin && !isTeacher) {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect("/login?roleError=1");
                        return;
                    }

                    if ("student".equalsIgnoreCase(loginPortal) && !isStudent) {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect("/student-login?roleError=1");
                        return;
                    }

                    response.sendRedirect("/dashboard");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        http.addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(firstLoginEnforcementFilter, JwtAuthenticationFilter.class);
        http.addFilterAfter(tenantContextFilter, JwtAuthenticationFilter.class);
        http.addFilterAfter(adminActionAuditFilter, TenantContextFilter.class);

        return http.build();
    }
}
