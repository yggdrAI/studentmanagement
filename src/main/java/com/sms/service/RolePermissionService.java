package com.sms.service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sms.config.TenantContext;
import com.sms.model.Permission;
import com.sms.model.Role;

@Service("permissionEngine")
public class RolePermissionService {

    private final Map<Role, Set<Permission>> rolePermissions;

    public RolePermissionService() {
        Map<Role, Set<Permission>> matrix = new EnumMap<>(Role.class);

        matrix.put(Role.ADMIN, Set.of(
            Permission.VIEW_ANALYTICS,
            Permission.CREATE_TIMETABLE,
            Permission.EDIT_TIMETABLE,
            Permission.MARK_ATTENDANCE,
            Permission.MANAGE_STUDENTS,
            Permission.MANAGE_TEACHERS,
            Permission.EXPORT_REPORTS,
            Permission.MANAGE_SYSTEM
        ));

        matrix.put(Role.TEACHER, Set.of(
            Permission.VIEW_ANALYTICS,
            Permission.EDIT_TIMETABLE,
            Permission.MARK_ATTENDANCE
        ));

        matrix.put(Role.STUDENT, Set.of(
            Permission.VIEW_ANALYTICS
        ));

        this.rolePermissions = Collections.unmodifiableMap(matrix);
    }

    public Set<Permission> getPermissions(Role role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return rolePermissions.getOrDefault(role, Collections.emptySet());
    }

    public Set<String> getPermissionNames(Role role) {
        return getPermissions(role).stream()
            .map(Enum::name)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean canAccessTenant(Long tenantId) {
        if (tenantId == null) {
            return true;
        }
        Long currentTenant = TenantContext.get();
        return currentTenant != null && currentTenant.equals(tenantId);
    }

    public boolean sameUser(String username, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || username == null) {
            return false;
        }
        return username.equalsIgnoreCase(authentication.getName());
    }
}
