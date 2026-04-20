package com.sms.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sms.dto.auth.ChangePasswordRequest;
import com.sms.dto.auth.LoginRequest;
import com.sms.dto.auth.LoginResponse;
import com.sms.model.Role;
import com.sms.model.Student;
import com.sms.model.User;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.UserRepository;
import com.sms.service.CredentialService;
import com.sms.service.JwtService;
import com.sms.service.RolePermissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final RolePermissionService rolePermissionService;
    private final CredentialService credentialService;

    public ApiAuthController(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             UserRepository userRepository,
                             StudentProfileRepository studentProfileRepository,
                             StudentRepository studentRepository,
                             RolePermissionService rolePermissionService,
                             CredentialService credentialService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentRepository = studentRepository;
        this.rolePermissionService = rolePermissionService;
        this.credentialService = credentialService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String normalizedUsername = request.getUsername() == null ? "" : request.getUsername().trim();
        User user = resolveUserByLoginIdentifier(normalizedUsername)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Account is locked. Try again after " + user.getAccountLockedUntil());
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            registerFailedAttempt(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        resetFailedAttemptState(user);

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

        boolean firstLoginRequired = Boolean.TRUE.equals(user.getIsFirstLogin());

        return ResponseEntity.ok(new LoginResponse(token, role, tenantId, permissions, expiresAt, firstLoginRequired));
    }

    private Optional<User> resolveUserByLoginIdentifier(String loginIdentifier) {
        Optional<User> directUser = userRepository.findByUsername(loginIdentifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(loginIdentifier));
        if (directUser.isPresent()) {
            return directUser;
        }

        return studentProfileRepository.findByEnrollmentNumberIgnoreCase(loginIdentifier)
                .flatMap(profile -> studentRepository.findById(profile.getStudentId()))
                .map(Student::getUser)
                .filter(java.util.Objects::nonNull);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        credentialService.changePassword(
                authentication.getName(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(java.util.Map.of(
                "username", user.getUsername(),
                "role", user.getRole() == null ? "STUDENT" : user.getRole().name(),
                "isFirstLogin", Boolean.TRUE.equals(user.getIsFirstLogin()),
                "isActive", !Boolean.FALSE.equals(user.getIsActive())
        ));
    }

    private void registerFailedAttempt(User user) {
        Integer existingAttempts = user.getFailedLoginAttempts();
        int attempts = existingAttempts == null ? 0 : existingAttempts.intValue();
        attempts += 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
            user.setFailedLoginAttempts(0);
        }

        userRepository.save(user);
    }

    private void resetFailedAttemptState(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }
}
