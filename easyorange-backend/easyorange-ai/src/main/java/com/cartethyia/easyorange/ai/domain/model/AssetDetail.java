package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** 在售资产详情 — product_detail 工具的观察物（多步循环里对某件候选的深入查看）；price 为 null 即面议，status 供在售判定。 */
public record AssetDetail(
        String productId,
        String title,
        @Nullable String description,
        @Nullable BigDecimal price,
        @Nullable String categoryName,
        @Nullable String conditionDesc,
        @Nullable String location,
        @Nullable String sellerName,
        @Nullable String status) {}
