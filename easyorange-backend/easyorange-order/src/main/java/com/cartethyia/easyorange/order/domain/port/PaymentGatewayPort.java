package com.cartethyia.easyorange.order.domain.port;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    String createPayment(CreatePaymentRequest request);

    /**
     * 发起支付 — 由支付模块执行「准备 → 网关 → 确认」两阶段，支付成功后经
     * {@code PaymentSucceededEvent} 事件桥接回订单侧置 PAID。
     * <p>
     * 支付单缺失时由支付模块抛业务异常 B4001（记录不存在）；订单与支付单在同一下单事务内
     * 落库，正常路径下不会缺失，该分支即数据不一致的显性失败信号。
     */
    void pay(String orderId);

    /**
     * 退款 — 操作者记为支付单所属用户，供订单取消等系统内部路径使用；支付单缺失同样抛 B4001。
     */
    void refundPayment(String orderId, String reason);

    record CreatePaymentRequest(
            String orderId,
            BigDecimal amount,
            String paymentMethod,
            String attach,
            String description,
            String buyerId) {}
}
