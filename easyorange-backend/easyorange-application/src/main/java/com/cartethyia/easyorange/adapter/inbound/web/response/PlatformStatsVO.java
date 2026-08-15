package com.cartethyia.easyorange.adapter.inbound.web.response;

/**
 * 平台统计视图 — 运维大盘三项指标。
 */
public record PlatformStatsVO(long activeUsers, long onlineProducts, long completedOrders) {}
