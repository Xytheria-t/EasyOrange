package com.cartethyia.easyorange.common.security;

import java.util.Set;
import lombok.Builder;

@Builder
public record AuthUser(String userId, String username, Set<String> roles, Set<String> permissions, Long loginTime) {

    public AuthUser {
        roles = roles == null || roles.isEmpty() ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null || permissions.isEmpty() ? Set.of() : Set.copyOf(permissions);
    }
}
