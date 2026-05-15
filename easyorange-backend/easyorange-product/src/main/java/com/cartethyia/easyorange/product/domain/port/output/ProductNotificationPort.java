package com.cartethyia.easyorange.product.domain.port.output;

/**
 * 商品事件通知端口
 *
 * <p>用于商品业务事件发生后向用户发送系统通知。
 * 由 application 模块通过 MessageCommandHandler 实现，
 * product 模块内无此依赖，通过端口接口 + Optional 注入解耦。</p>
 */
public interface ProductNotificationPort extends OutboundPort {

    /**
     * 通知卖家商品已发布成功
     */
    void notifyProductCreated(Long productId, Long userId);

    /**
     * 通知卖家商品已售出
     */
    void notifyProductMarkedSold(Long productId);

    /**
     * 通知卖家/管理员库存不足（低于预警阈值）
     */
    void notifyLowStock(Long productId, int currentStock);
}
