package com.cartethyia.easyorange.common.dto;

import lombok.Builder;
import java.util.Collections;
import java.util.Set;

@Builder
public record AuthUser(
        Long userId,
        String username,
        Set<String> roles,
        Set<String> permissions,
        Long loginTime
) {

    public AuthUser {
        roles = roles == null || roles.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(Set.copyOf(roles));
        permissions = permissions == null || permissions.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(Set.copyOf(permissions));
    }
}