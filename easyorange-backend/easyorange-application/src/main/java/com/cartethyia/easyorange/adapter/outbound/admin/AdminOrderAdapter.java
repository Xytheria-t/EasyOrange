package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemMapper;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.application.port.query.OrderQueryRepository;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.repository.OrderRepository;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 订单查询/操作适配器
 * <p>
 * 实现 {@link AdminOrderPort}，通过 Order Mapper / Repository 访问订单数据并转换为 Admin 模块需要的格式。
 * 状态字段使用 String code，由 OrderDO 的 enum 字段直接 {@code getCode()} 派生。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminOrderAdapter implements AdminOrderPort {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final PaymentMapper paymentMapper;
    private final OrderQueryRepository orderReadRepository;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public OrderQueryResult queryOrders(OrderQueryCondition condition) {
        var wrapper = ChainWrappers.lambdaQueryChain(orderMapper).eq(OrderDO::getDelFlag, 0);

        if (condition.orderNo() != null && !condition.orderNo().isEmpty()) {
            wrapper.like(OrderDO::getOrderNo, condition.orderNo());
        }
        if (condition.buyerId() != null) {
            wrapper.eq(OrderDO::getBuyerId, condition.buyerId());
        }
        if (condition.sellerId() != null) {
            wrapper.eq(OrderDO::getSellerId, condition.sellerId());
        }
        if (condition.status() != null) {
            wrapper.eq(OrderDO::getStatus, parseOrderStatus(condition.status()));
        }
        if (condition.paymentStatus() != null) {
            wrapper.eq(OrderDO::getPaymentStatus, parsePaymentStatus(condition.paymentStatus()));
        }
        if (condition.startTime() != null) {
            wrapper.ge(OrderDO::getCreateTime, condition.startTime());
        }
        if (condition.endTime() != null) {
            wrapper.le(OrderDO::getCreateTime, condition.endTime());
        }

        wrapper.orderByDesc(OrderDO::getCreateTime);

        int pageNum = condition.pageNum() != null ? condition.pageNum() : 1;
        int pageSize = condition.pageSize() != null ? condition.pageSize() : 20;
        Page<OrderDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<OrderSummary> records =
                page.getRecords().stream().map(this::toOrderSummary).collect(Collectors.toList());

        return new OrderQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<String, List<OrderItemInfo>> getOrderItems(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItemDO> items = ChainWrappers.lambdaQueryChain(orderItemMapper)
                .in(OrderItemDO::getOrderId, orderIds)
                .list();
        return items.stream()
                .collect(Collectors.groupingBy(
                        OrderItemDO::getOrderId, Collectors.mapping(this::toOrderItemInfo, Collectors.toList())));
    }

    @Override
    public Map<String, ProductInfo> getProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectByIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductDO::getId, this::toProductInfo, (a, b) -> a));
    }

    @Override
    public OrderDetail getOrderDetail(String orderId) {
        return orderReadRepository
                .findById(OrderId.of(orderId))
                .map(this::toOrderDetail)
                .orElse(null);
    }

    @Override
    public OrderStats getOrderStats() {
        Map<String, Long> countsByStatus = orderMapper
                .selectMaps(new QueryWrapper<OrderDO>()
                        .select("status", "COUNT(*) AS cnt")
                        .eq("del_flag", 0)
                        .groupBy("status"))
                .stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("status")), row -> ((Number) row.get("cnt")).longValue()));

        long totalOrders =
                countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        long todayOrders =
                orderReadRepository.countByCreatedAfter(LocalDate.now().atStartOfDay());

        return new OrderStats(
                totalOrders,
                todayOrders,
                countsByStatus.getOrDefault(OrderStatus.PENDING_PAYMENT.getCode(), 0L),
                countsByStatus.getOrDefault(OrderStatus.PAID.getCode(), 0L),
                countsByStatus.getOrDefault(OrderStatus.SHIPPED.getCode(), 0L),
                countsByStatus.getOrDefault(OrderStatus.COMPLETED.getCode(), 0L),
                countsByStatus.getOrDefault(OrderStatus.CANCELLED.getCode(), 0L),
                countsByStatus.getOrDefault(OrderStatus.REFUNDED.getCode(), 0L),
                sumSuccessfulPayments(null),
                sumSuccessfulPayments(LocalDate.now().atStartOfDay()));
    }

    /**
     * 营收口径：eo_payment 中状态 SUCCESS 的支付金额合计；今日营收按支付完成时间（update_time）过滤。
     */
    private BigDecimal sumSuccessfulPayments(LocalDateTime since) {
        QueryWrapper<PaymentDO> wrapper = new QueryWrapper<PaymentDO>()
                .select("IFNULL(SUM(amount), 0) AS total")
                .eq("del_flag", 0)
                .eq("status", "SUCCESS");
        if (since != null) {
            wrapper.ge("update_time", since);
        }
        return paymentMapper.selectMaps(wrapper).stream()
                .map(row -> (BigDecimal) row.get("total"))
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public void cancelOrder(String orderId, String reason) {
        var aggregate = findOrderOrThrow(orderId);

        if (aggregate.status() == OrderStatus.PENDING_PAYMENT) {
            persistAndPublish(aggregate.cancel(reason, LocalDateTime.now()));
        } else if (aggregate.status() == OrderStatus.PAID) {
            persistAndPublish(aggregate.forceCancel(reason, LocalDateTime.now()));
        } else {
            throw BusinessException.of("当前订单状态不允许取消");
        }
    }

    @Override
    public void forceComplete(String orderId) {
        var aggregate = findOrderOrThrow(orderId);
        persistAndPublish(aggregate.confirmReceipt(LocalDateTime.now()));
    }

    @Override
    public void refundOrder(String orderId, String reason) {
        var aggregate = findOrderOrThrow(orderId);
        persistAndPublish(aggregate.refund(reason, LocalDateTime.now()));
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(OrderId.of(orderId)).orElseThrow(() -> BusinessException.of("订单不存在"));
    }

    private static OrderStatus parseOrderStatus(String code) {
        try {
            return OrderStatus.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw BusinessException.of("无效的订单状态");
        }
    }

    private static PaymentStatus parsePaymentStatus(String code) {
        try {
            return PaymentStatus.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw BusinessException.of("无效的支付状态");
        }
    }

    private void persistAndPublish(com.cartethyia.easyorange.common.event.Transition<Order, ?> result) {
        orderRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    private OrderDetail toOrderDetail(OrderReadModel model) {
        List<OrderItemDetail> items = model.items().stream()
                .map(item -> new OrderItemDetail(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new OrderDetail(
                model.id(),
                model.orderNo(),
                model.buyerId(),
                model.sellerId(),
                items,
                model.totalAmount(),
                model.status(),
                model.statusDesc(),
                model.paymentStatus(),
                model.remark(),
                model.cancelReason(),
                model.createTime(),
                model.updateTime(),
                model.cancelTime(),
                model.refundReason(),
                model.refundTime());
    }

    private OrderSummary toOrderSummary(OrderDO order) {
        OrderStatus status = order.getStatus();
        PaymentStatus paymentStatus = order.getPaymentStatus();
        return new OrderSummary(
                order.getId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalAmount(),
                status != null ? status.getCode() : null,
                status != null ? status.getDesc() : "未知状态",
                paymentStatus != null ? paymentStatus.getCode() : null,
                paymentStatus != null ? paymentStatus.getDesc() : "未支付",
                order.getCreateTime());
    }

    private OrderItemInfo toOrderItemInfo(OrderItemDO item) {
        return new OrderItemInfo(item.getOrderId(), item.getProductId(), item.getQuantity(), item.getUnitPrice());
    }

    private ProductInfo toProductInfo(ProductDO product) {
        return new ProductInfo(product.getId(), product.getName(), product.getPrice());
    }
}
