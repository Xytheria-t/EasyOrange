package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderStatsResponse(
    long totalOrders,
    long todayOrders,
    long pendingPayment,
    long toShip,
    long toReceive,
    long completed,
    long cancelled,
    long refunded,
    BigDecimal totalRevenue,
    BigDecimal todayRevenue
) {}