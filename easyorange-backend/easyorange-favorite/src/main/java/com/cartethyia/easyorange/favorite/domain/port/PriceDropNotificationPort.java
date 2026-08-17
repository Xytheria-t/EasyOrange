package com.cartethyia.easyorange.favorite.domain.port;

import java.math.BigDecimal;

/**
 * 降价通知出口 — 由 application 组装层适配到站内信等通知通道。
 * <p>
 * favorite 模块不感知 message 模块存在，通过本 Port 保持业务模块间仅端口通信。
 */
public interface PriceDropNotificationPort {

    void sendPriceDropNotification(
            String userId, String productId, String productName, BigDecimal oldPrice, BigDecimal newPrice);
}
