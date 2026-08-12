package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户访问器。
 * <p>
 * 约定：{@code SecurityContextHolder} 的 principal 恒为 {@link AuthUser}
 * （由 {@code SecurityConfig#jwtAuthenticationConverter} 注入，测试用 {@link TestSecurityUtil}）。
 * 匿名请求（permitAll 路径）视为未登录，返回 {@code empty}。
 */
@UtilityClass
public class SecurityContextUtil {

    // ==================== Current User ID (High-level API) ====================

    /**
     * 获取当前用户ID，未登录时返回 empty。
     */
    public static Optional<String> getCurrentUserId() {
        return getUserContext().map(AuthUser::userId);
    }

    // ==================== User Context (Core API) ====================

    /**
     * 获取当前登录用户上下文，未登录时返回 empty。
     */
    public static Optional<AuthUser> getUserContext() {
        return getAuthentication().flatMap(SecurityContextUtil::extractUser);
    }

    /**
     * 获取当前登录用户上下文，未登录时抛出业务异常。
     */
    public static AuthUser getUserContextOrThrow() {
        return getUserContext().orElseThrow(() -> BusinessException.of(ResultCode.UNAUTHORIZED, "用户未登录"));
    }

    // ==================== Context Management ====================

    /**
     * 清除当前线程的安全上下文（通常在请求结束或测试清理时调用）。
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Private Helpers (Foundation) ====================

    /**
     * 获取当前有效的 Authentication。
     * <p>
     * 过滤掉 null、未认证、以及 AnonymousAuthenticationToken。
     */
    private static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                // AnonymousAuthenticationToken.isAuthenticated() == true，必须显式排除
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken));
    }

    /**
     * 约定 principal 恒为 {@link AuthUser}（见类注释），否则为配置错误，快速失败。
     */
    private static Optional<AuthUser> extractUser(Authentication auth) {
        if (auth.getPrincipal() instanceof AuthUser user) {
            return Optional.of(user);
        }
        throw new IllegalStateException("Authentication principal 必须是 AuthUser，实际为 " + auth.getPrincipal());
    }
}
