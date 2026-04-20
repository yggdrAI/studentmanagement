package com.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sms.model.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final IdentityLookupService identityLookupService;
    private final RolePermissionService rolePermissionService;

    public CustomUserDetailsService(IdentityLookupService identityLookupService,
                                    RolePermissionService rolePermissionService) {
        this.identityLookupService = identityLookupService;
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username == null ? "" : username.trim();
        User user = identityLookupService.findByLoginIdentifier(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        rolePermissionService.getPermissionNames(user.getRole())
            .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .disabled(Boolean.FALSE.equals(user.getIsActive()))
                .accountLocked(user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now()))
                .authorities(authorities)
                .build();
    }
}
