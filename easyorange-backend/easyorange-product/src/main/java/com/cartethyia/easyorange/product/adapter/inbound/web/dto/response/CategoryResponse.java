package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;

/**
 * C 端分类响应。
 * <p>
 * 删掉了恒为 null 的 {@code children}：树结构只在 admin 的 {@code /tree} 用，
 * 这里返回的是平铺的一层，前端也从未读过它。
 * <p>
 * {@code status} 在 DTO 层是 0/1 数字而非领域枚举：web DTO 是对外契约的边界，
 * 枚举的 {@code @JsonValue} 会把它序列化成字符串 {@code "1"}，与 admin 侧的
 * 数字状态对不上。这里显式转数字，两个出口就是同一套契约。
 */
public record CategoryResponse(
        String id,
        String name,
        /** 一级分类为 null。 */
        String parentId,
        Integer level,
        String icon,
        Integer sortOrder,
        Integer status,
        LocalDateTime createTime,
        Integer productCount) {}
