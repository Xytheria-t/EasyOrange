package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** 在售资产详情 — product_detail 工具的观察物（多步循环对某件候选的深入查看）；status 在售状态模型据此判断是否还推荐得出口，price 为 null 即面议。 */
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
