package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.domain.Version;
import com.cartethyia.easyorange.framework.util.Jsons;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItemSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderDataMapper {

    private final ObjectMapper objectMapper;

    public OrderDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ── DO → 聚合根（读路径） ──

    public Order toAggregate(OrderDO orderDO, List<OrderItem> items) {
        if (orderDO == null) return null;
        return Order.from(toReconstructSpec(orderDO, items != null ? items : List.of()));
    }

    // ── DO → 读模型 ──

    public OrderReadModel toReadModel(OrderDO orderDO, List<OrderItemReadModel> items) {
        if (orderDO == null) return null;
        var status = orderDO.getStatus();
        var paymentStatus = orderDO.getPaymentStatus();
        return new OrderReadModel(
                orderDO.getId(),
                orderDO.getOrderNo(),
                orderDO.getBuyerId(),
                orderDO.getSellerId(),
                items != null ? items : List.of(),
                orderDO.getTotalAmount(),
                status != null ? status.getCode() : null,
                status != null ? status.getDesc() : null,
                paymentStatus != null ? paymentStatus.getCode() : null,
                orderDO.getAddress(),
                orderDO.getPhone(),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime(),
                orderDO.getRefundReason(),
                orderDO.getRefundTime(),
                orderDO.getCreateTime(),
                orderDO.getUpdateTime());
    }

    // ── 订单项 DO → 领域对象 ──

    public OrderItem toOrderItem(OrderItemDO itemDO) {
        if (itemDO == null) return null;
        return OrderItem.builder()
                .id(itemDO.getId())
                .productId(ProductId.of(itemDO.getProductId()))
                .snapshot(fromJson(itemDO.getProductSnapshot()))
                .unitPrice(Money.of(itemDO.getUnitPrice()))
                .quantity(itemDO.getQuantity())
                .subtotal(Money.of(itemDO.getSubtotal()))
                .build();
    }

    // ── 订单项 DO → 读模型 ──

    public OrderItemReadModel toItemReadModel(OrderItemDO itemDO) {
        if (itemDO == null) return null;
        return new OrderItemReadModel(
                itemDO.getId(),
                itemDO.getProductId(),
                fromJson(itemDO.getProductSnapshot()),
                itemDO.getUnitPrice(),
                itemDO.getQuantity(),
                itemDO.getSubtotal());
    }

    // ── 聚合根 → DO（写路径） ──

    public OrderDO toDataObject(Order aggregate) {
        if (aggregate == null) return null;
        return OrderDO.builder()
                .id(aggregate.id().value())
                .orderNo(aggregate.orderNo().value())
                .buyerId(aggregate.buyerId().value())
                .sellerId(aggregate.sellerId().value())
                .totalAmount(aggregate.totalAmount().value())
                .status(aggregate.status())
                .paymentStatus(aggregate.paymentStatus())
                .address(aggregate.address().value())
                .phone(aggregate.phone().value())
                .remark(aggregate.remark())
                .cancelReason(aggregate.cancelReason())
                .cancelTime(aggregate.cancelTime())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .version(aggregate.version() != null ? aggregate.version().value() : null)
                .build();
    }

    public OrderItemDO toItemDO(String orderId, OrderItem item) {
        if (item == null) return null;
        return OrderItemDO.builder()
                .id(item.id())
                .orderId(orderId)
                .productId(item.productId().value())
                .productSnapshot(toJson(item.snapshot()))
                .unitPrice(item.unitPrice().value())
                .quantity(item.quantity())
                .subtotal(item.subtotal().value())
                .build();
    }

    // ── 共用辅助方法 ──

    private static OrderReconstructSpec toReconstructSpec(OrderDO orderDO, List<OrderItem> items) {
        return new OrderReconstructSpec(
                OrderId.of(orderDO.getId()),
                OrderNo.of(orderDO.getOrderNo()),
                UserId.of(orderDO.getBuyerId()),
                UserId.of(orderDO.getSellerId()),
                items,
                Money.of(orderDO.getTotalAmount()),
                orderDO.getStatus(),
                orderDO.getPaymentStatus(),
                Address.of(orderDO.getAddress()),
                Phone.of(orderDO.getPhone()),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime(),
                orderDO.getRefundReason(),
                orderDO.getRefundTime(),
                Version.of(orderDO.getVersion()));
    }

    private String toJson(OrderItemSnapshot snapshot) {
        return Jsons.writeRequired(objectMapper, snapshot, "Failed to serialize OrderItemSnapshot");
    }

    private OrderItemSnapshot fromJson(String json) {
        return Jsons.readRequired(
                objectMapper, json, OrderItemSnapshot.class, "Failed to deserialize OrderItemSnapshot");
    }
}
