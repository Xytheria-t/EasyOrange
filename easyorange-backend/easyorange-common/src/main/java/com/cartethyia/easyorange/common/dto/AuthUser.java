package com.cartethyia.easyorange.common.dto;

import java.util.Collections;
import java.util.Set;

public record AuthUser(
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        Long loginTime
) {

    public AuthUser {
        if (roles == null) {
            roles = Collections.emptySet();
        }
        if (permissions == null) {
            permissions = Collections.emptySet();
        }
    }

    public static AuthUserBuilder builder() {
        return new AuthUserBuilder();
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... roleList) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return java.util.Arrays.stream(roleList).anyMatch(roles::contains);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... permissionList) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return java.util.Arrays.stream(permissionList).anyMatch(permissions::contains);
    }

    public static class AuthUserBuilder {
        private Long userId;
        private String username;
        private Set<String> roles = Collections.emptySet();
        private Set<String> permissions = Collections.emptySet();
        private Long loginTime;

        public AuthUserBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AuthUserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthUserBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public AuthUserBuilder permissions(Set<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public AuthUserBuilder loginTime(Long loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        public AuthUser build() {
            return new AuthUser(userId, username, roles, permissions, loginTime);
        }
    }
}