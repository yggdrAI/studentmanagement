package com.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sms.model.Student;
import com.sms.model.User;
import com.sms.repository.StudentProfileRepository;
import com.sms.repository.StudentRepository;
import com.sms.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final RolePermissionService rolePermissionService;

    public CustomUserDetailsService(UserRepository userRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    StudentRepository studentRepository,
                                    RolePermissionService rolePermissionService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentRepository = studentRepository;
        this.rolePermissionService = rolePermissionService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username == null ? "" : username.trim();
        User user = resolveUserByLoginIdentifier(normalizedUsername)
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
}
