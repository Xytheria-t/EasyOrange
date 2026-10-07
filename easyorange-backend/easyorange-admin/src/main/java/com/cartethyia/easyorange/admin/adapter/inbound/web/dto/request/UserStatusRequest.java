package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理端改用户状态。取值面固定，绑定期即拒非法值——用 {@code @Pattern} 而不是引 user 模块的
 * {@code UserStatus} 枚举，因为 admin 对业务模块零依赖（见 easyorange-backend/AGENTS.md「模块要点 → admin」）。
 */
public record UserStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "NORMAL|DISABLED|LOCKED", message = "状态只能是 NORMAL / DISABLED / LOCKED")
        String status,

        String reason) {}
