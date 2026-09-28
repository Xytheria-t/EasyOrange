package com.cartethyia.easyorange.admin.domain.model;

/**
 * 仪表板趋势上的一个自然月 — 缺数据的月份补 0（前端折线不断点）。
 *
 * @param month    {@code yyyy-MM}
 * @param users    当月新增用户
 * @param products 当月新增商品
 * @param orders   当月新增订单
 */
public record TrendPoint(String month, long users, long products, long orders) {}
