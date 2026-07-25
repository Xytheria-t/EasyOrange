package com.cartethyia.easyorange.favorite.domain.aggregate;

/**
 * Favorite 聚合根工厂参数对象 — 创建场景。
 * <p>
 * 收敛 create() 参数为单一 record，对齐 product/order/payment 模块的 Spec 模式。
 * 纯 VO 字段，domain 层零框架依赖。
 */
public record FavoriteCreateSpec(String userId, String productId) {}
