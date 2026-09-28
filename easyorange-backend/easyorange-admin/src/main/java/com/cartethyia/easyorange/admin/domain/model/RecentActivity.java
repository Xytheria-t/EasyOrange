package com.cartethyia.easyorange.admin.domain.model;

import java.time.LocalDateTime;

/**
 * 仪表板「最近动态」条目 — 跨模块聚合的只读视图（用户注册 / 商品发布 / 订单创建共用同一形状）。
 * <p>
 * 归到 domain 而非各自端口的 record：三条流水线的展示层口径完全一致（按时间倒序取若干条、
 * 取一个可展示的名字），各端口各定义一份只会把同构数据复制三遍。
 *
 * @param id        资源 ID（用户 / 商品 / 订单）
 * @param display   展示用名称（昵称 / 商品名 / 订单号）
 * @param createdAt 创建时间
 */
public record RecentActivity(String id, String display, LocalDateTime createdAt) {}
