package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.aggregate.OrderReconstructSpec;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.Address;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.Phone;
import com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderDataMapper {

    private final ObjectMapper objectMapper;

    public OrderDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== DO → Aggregate (Read path) ====================

    public Order toAggregate(OrderDO orderDO) {
        if (orderDO == null) return null;
        return Order.from(toReconstructSpec(orderDO, List.of()));
    }

    public Order toAggregate(OrderDO orderDO, List<OrderItem> items) {
        if (orderDO == null) return null;
        return Order.from(toReconstructSpec(orderDO, items != null ? items : List.of()));
    }

    // ==================== DO → ReadModel ====================

    public OrderReadModel toReadModel(OrderDO orderDO) {
        if (orderDO == null) return null;
        return toReadModel(orderDO, List.of());
    }

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
                orderDO.getCreateTime(),
                orderDO.getUpdateTime());
    }

    // ==================== Item DO → Domain ====================

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

    // ==================== Item DO → ItemReadModel ====================

    public OrderItemReadModel toItemReadModel(OrderItemDO itemDO) {
        if (itemDO == null) return null;
        return new OrderItemReadModel(
                itemDO.getId(),
                itemDO.getProductId(),
                itemDO.getProductSnapshot(),
                itemDO.getUnitPrice(),
                itemDO.getQuantity(),
                itemDO.getSubtotal());
    }

    // ==================== Aggregate → DO (Write path) ====================

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

    // ==================== Shared helpers ====================

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
                orderDO.getCancelTime());
    }

    private String toJson(ProductSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, "Failed to serialize ProductSnapshot", e);
        }
    }

    private ProductSnapshot fromJson(String json) {
        try {
            return objectMapper.readValue(json, ProductSnapshot.class);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, "Failed to deserialize ProductSnapshot", e);
        }
    }
}
