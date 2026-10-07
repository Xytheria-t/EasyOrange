package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理端改商品状态。取值面固定，绑定期即拒非法值——用 {@code @Pattern} 而不是引 product 模块的
 * {@code ProductStatus} 枚举，因为 admin 对业务模块零依赖（见 easyorange-backend/AGENTS.md「模块要点 → admin」）。
 * 合法转换（而非合法取值）由 product 侧聚合守卫裁决，这里只管取值面。
 * <p>
 * 无 {@code reason} 字段：本路径的操作人不落库（{@code AdminProductPort.applyProductStatus} 只收状态码），
 * 收一个用不上的字段等于给调用方一个不生效的旋钮。
 */
public record ProductStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ONLINE|OFFLINE|SOLD", message = "状态只能是 ONLINE / OFFLINE / SOLD")
        String status) {}
