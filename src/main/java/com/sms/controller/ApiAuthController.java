package com.sms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sms.dto.auth.LoginRequest;
import com.sms.dto.auth.LoginResponse;
import com.sms.model.Role;
import com.sms.model.User;
import com.sms.repository.UserRepository;
import com.sms.service.JwtService;
import com.sms.service.RolePermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RolePermissionService rolePermissionService;

    public ApiAuthController(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             UserRepository userRepository,
                             RolePermissionService rolePermissionService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.rolePermissionService = rolePermissionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long tenantId = userRepository.findByUsername(userDetails.getUsername())
            .map(User::getTenantId)
            .orElse(1L);
        String role = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .findFirst()
            .orElse("STUDENT");
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role);
        } catch (IllegalArgumentException ignored) {
            roleEnum = Role.STUDENT;
        }
        var permissions = rolePermissionService.getPermissionNames(roleEnum).stream().toList();

        String token = jwtService.generateToken(userDetails.getUsername(), role, tenantId, permissions);
        long expiresAt = jwtService.extractExpiration(token).getTime();

        return ResponseEntity.ok(new LoginResponse(token, role, tenantId, permissions, expiresAt));
    }
}
