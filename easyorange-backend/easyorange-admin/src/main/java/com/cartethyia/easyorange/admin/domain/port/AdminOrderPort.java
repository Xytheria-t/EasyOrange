package com.cartethyia.easyorange.admin.domain.port;

import com.cartethyia.easyorange.admin.domain.model.RecentActivity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的订单查询/操作端口 — 跨模块查询与操作订单信息的唯一出口。
 * <p>
 * <b>取舍</b>：{@code reason} 只出现在**会被持久化**的干预动作上（取消 / 退款），
 * 走 order 侧的订单原因字段；不落库的动作（强制完成）不收它，收了就是装饰。
 * <p>
 * <b>边界</b>：查询一律只读，越权与状态非法由 order 模块的领域异常决定，端口只翻译不吞。
 */
public interface AdminOrderPort {

    OrderQueryResult queryOrders(OrderQueryCondition condition);

    Map<String, List<OrderItemInfo>> getOrderItems(List<String> orderIds);

    Map<String, ProductInfo> getProducts(List<String> productIds);

    OrderDetail getOrderDetail(String orderId);

    OrderStats getOrderStats();

    /**
     * 下单趋势：{@code yyyy-MM} → 新增订单数，键按创建时间升序
     */
    Map<String, Long> getCreateTrend(LocalDate since);

    /**
     * 最近创建的订单（按创建时间倒序取 limit 条）
     */
    List<RecentActivity> findRecentCreated(int limit);

    /**
     * 取消订单（PENDING_PAYMENT 取消 / PAID 强制取消），不合法状态抛出 BusinessException
     */
    void cancelOrder(String orderId, String reason);

    /**
     * 强制完成订单（确认收货），不合法状态抛出 BusinessException
     */
    void forceComplete(String orderId);

    /**
     * 退款订单，不合法状态抛出 BusinessException
     */
    void refundOrder(String orderId, String reason);

    /**
     * 订单查询条件 — status/paymentStatus 为 String code（与 OrderStatus/PaymentStatus code 一致）。
     */
    record OrderQueryCondition(
            String orderNo,
            String buyerId,
            String sellerId,
            String status,
            String paymentStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    record OrderQueryResult(List<OrderSummary> records, long total, int pageNum, int pageSize) {}

    /**
     * 订单摘要信息 — status/paymentStatus 为 String code。
     */
    record OrderSummary(
            String id,
            String orderNo,
            String buyerId,
            String sellerId,
            BigDecimal totalAmount,
            String status,
            String statusDesc,
            String paymentStatus,
            String paymentStatusDesc,
            LocalDateTime createTime) {}

    record OrderItemInfo(String orderId, String productId, Integer quantity, BigDecimal price) {}

    record ProductInfo(String id, String name, BigDecimal price) {}

    /**
     * 订单详情读模型。
     *
     * <p><b>支付字段的来源与边界</b>：{@code paymentNo} / {@code paidAmount} / {@code refundedAmount} /
     * {@code payTime} 来自 {@code eo_payment} 表，按 {@code orderId} 取该订单的支付单；订单尚未发起支付时
     * 四项均为 {@code null}（不是 0 —— 「没付过」与「付了 0 元」在运营处置上完全不同，前者要催付、后者不可能发生）。
     * {@code payTime} 取支付单的 {@code updateTime}：该表无独立的支付时间列，支付成功是这条记录最后一次
     * 状态变更，故用 {@code updateTime} 近似，列语义与 {@code eo_order.payment_status} 保持同一时点。
     *
     * <p><b>收货信息</b>：{@code eo_order} 只有 {@code address} / {@code phone} 两列，没有收件人姓名单列
     * （下单时只校验买家本人，地址与买家档案同源），故收件人名取买家昵称。
     */
    record OrderDetail(
            String id,
            String orderNo,
            String buyerId,
            String sellerId,
            List<OrderItemDetail> items,
            BigDecimal totalAmount,
            String status,
            String statusDesc,
            String paymentStatus,
            String paymentNo,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            String address,
            String phone,
            String remark,
            String cancelReason,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            LocalDateTime payTime,
            LocalDateTime cancelTime,
            String refundReason,
            LocalDateTime refundTime) {}

    /** 行项自带 id 与小计 —— 详情页要按行渲染，不能只给 productId 让前端自己拼。 */
    record OrderItemDetail(String itemId, String productId, Integer quantity, BigDecimal price, BigDecimal subtotal) {}

    /**
     * 订单状态统计。营收口径：eo_payment 中状态 SUCCESS 的支付金额合计。
     */
    record OrderStats(
            long totalOrders,
            long todayOrders,
            long pendingPayment,
            long toShip,
            long toReceive,
            long completed,
            long cancelled,
            long refunded,
            BigDecimal totalRevenue,
            BigDecimal todayRevenue) {}
}
