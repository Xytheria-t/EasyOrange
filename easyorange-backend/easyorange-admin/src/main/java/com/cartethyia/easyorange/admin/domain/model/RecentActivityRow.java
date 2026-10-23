package com.cartethyia.easyorange.admin.domain.model;

import java.time.LocalDateTime;

/**
 * 仪表板「最近动态」的原始行 — 端口返回的只读记录（用户注册 / 商品发布 / 订单创建共用同一形状），
 * 不含文案；中文文案由 {@code AdminDashboardAppService} 拼成 {@link DashboardActivity}。
 * <p>
 * 归到 domain 而非各自端口的 record：三条流水线的展示层口径完全一致（按时间倒序取若干条、
 * 取一个可展示的名字），各端口各定义一份只会把同构数据复制三遍。
 *
 * @param id        资源 ID（用户 / 商品 / 订单）
 * @param display   展示用名称（昵称 / 商品名 / 订单号）
 * @param createdAt 创建时间
 */
public record RecentActivityRow(String id, String display, LocalDateTime createdAt) {}
