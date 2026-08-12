package com.cartethyia.easyorange.common.security;

/**
 * 当前登录用户身份（仅身份，不含角色/权限）。
 * <p>
 * 角色与权限由 JWT 的 {@code authorities} claim 解析为 Spring 授权，
 * 经 {@code @PreAuthorize} / {@code hasRole} 或路径规则生效，不在此重复保存。
 */
public record AuthUser(String userId, String username) {}
