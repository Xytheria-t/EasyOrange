package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderQueryPort;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemDO;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderItemMapper;
import com.cartethyia.easyorange.order.adapter.outbound.persistence.OrderMapper;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin 订单查询适配器
 * 实现 AdminOrderQueryPort，通过 Order Mapper 查询数据并转换为 Admin 模块需要的格式
 */
@Component
@RequiredArgsConstructor
public class AdminOrderQueryAdapter implements AdminOrderQueryPort {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public OrderQueryResult queryOrders(OrderQueryCondition condition) {
        var wrapper = ChainWrappers.lambdaQueryChain(orderMapper)
            .eq(OrderDO::getDelFlag, 0);

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
            wrapper.eq(OrderDO::getStatus, condition.status());
        }
        if (condition.paymentStatus() != null) {
            wrapper.eq(OrderDO::getPaymentStatus, condition.paymentStatus());
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

        List<OrderSummary> records = page.getRecords().stream()
            .map(this::toOrderSummary)
            .collect(Collectors.toList());

        return new OrderQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public Map<Long, List<OrderItemInfo>> getOrderItems(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItemDO> items = ChainWrappers.lambdaQueryChain(orderItemMapper)
            .in(OrderItemDO::getOrderId, orderIds)
            .list();
        return items.stream()
            .collect(Collectors.groupingBy(
                OrderItemDO::getOrderId,
                Collectors.mapping(this::toOrderItemInfo, Collectors.toList())
            ));
    }

    @Override
    public Map<Long, ProductInfo> getProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectBatchIds(productIds);
        return products.stream()
            .collect(Collectors.toMap(
                ProductDO::getId,
                this::toProductInfo,
                (a, b) -> a
            ));
    }

    private OrderSummary toOrderSummary(OrderDO order) {
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        return new OrderSummary(
            order.getId(),
            order.getOrderNo(),
            order.getBuyerId(),
            order.getSellerId(),
            order.getTotalAmount(),
            order.getStatus(),
            status != null ? status.getDesc() : "未知状态",
            order.getPaymentStatus(),
            getPaymentStatusDesc(order.getPaymentStatus()),
            order.getCreateTime()
        );
    }

    private OrderItemInfo toOrderItemInfo(OrderItemDO item) {
        return new OrderItemInfo(
            item.getOrderId(),
            item.getProductId(),
            item.getQuantity(),
            item.getUnitPrice()
        );
    }

    private ProductInfo toProductInfo(ProductDO product) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getPrice()
        );
    }

    private String getPaymentStatusDesc(Integer paymentStatus) {
        if (paymentStatus == null) return "未支付";
        return switch (paymentStatus) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            default -> "未知";
        };
    }
}