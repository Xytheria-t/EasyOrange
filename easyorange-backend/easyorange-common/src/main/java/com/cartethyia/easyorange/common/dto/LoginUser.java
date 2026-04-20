package com.cartethyia.easyorange.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * 登录用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色集合（不含 ROLE_ 前缀）
     */
    @Builder.Default
    private Set<String> roles = Collections.emptySet();

    /**
     * 权限集合
     */
    @Builder.Default
    private Set<String> permissions = Collections.emptySet();

    /**
     * 登录时间戳（毫秒）
     */
    private Long loginTime;

    // ==================== 便捷方法 ====================

    /**
     * 是否拥有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 是否拥有任意一个指定角色
     */
    public boolean hasAnyRole(String... roleList) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return java.util.Arrays.stream(roleList).anyMatch(roles::contains);
    }

    /**
     * 是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 是否拥有任意一个指定权限
     */
    public boolean hasAnyPermission(String... permissionList) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return java.util.Arrays.stream(permissionList).anyMatch(permissions::contains);
    }
}
