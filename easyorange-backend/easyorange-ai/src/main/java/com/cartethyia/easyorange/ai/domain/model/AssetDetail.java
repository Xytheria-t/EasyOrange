package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * 在售资产详情 — product_detail 工具的观察物：多步循环对某件候选资产的深入查看。
 *
 * @param productId     资产 ID
 * @param title         标题
 * @param description   详情描述
 * @param price         当前售价（null = 面议）
 * @param categoryName  类目名
 * @param conditionDesc 成色描述
 * @param location      所在地
 * @param sellerName    卖家名
 * @param status        在售状态（模型据此判断是否还推荐得出口）
 */
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
