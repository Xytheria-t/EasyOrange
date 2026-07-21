package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Mapper(componentModel = "spring")
@SuppressWarnings("unused")
public interface OrderEntityMapper {

    ObjectMapper JACKSON = new tools.jackson.databind.ObjectMapper();

    // ==================== DO → Aggregate (Read path) ====================

    /**
     * 不含行项的重建（用于列表查询等不需要行项的场景）。
     */
    default OrderAggregate toAggregate(OrderDO orderDO) {
        if (orderDO == null) return null;
        return OrderAggregate.fromRaw(
                orderDO.getId(), orderDO.getOrderNo(),
                orderDO.getBuyerId(), orderDO.getSellerId(),
                orderDO.getTotalAmount(), orderDO.getStatus(),
                orderDO.getPaymentStatus(), orderDO.getAddress(),
                orderDO.getPhone(), orderDO.getRemark(),
                orderDO.getCancelReason(), orderDO.getCancelTime()
        );
    }

    /**
     * 携带行项的重建（用于订单详情等需要行项的场景）。
     */
    default OrderAggregate toAggregate(OrderDO orderDO, List<OrderItem> items) {
        if (orderDO == null) return null;
        return OrderAggregate.fromRaw(
                orderDO.getId(), orderDO.getOrderNo(),
                orderDO.getBuyerId(), orderDO.getSellerId(),
                items != null ? items : List.of(),
                orderDO.getTotalAmount(), orderDO.getStatus(),
                orderDO.getPaymentStatus(), orderDO.getAddress(),
                orderDO.getPhone(), orderDO.getRemark(),
                orderDO.getCancelReason(), orderDO.getCancelTime()
        );
    }

    // ==================== DO → ReadModel ====================

    /**
     * 不含行项的 ReadModel 重建（用于列表查询）。
     */
    default OrderReadModel toReadModel(OrderDO orderDO) {
        if (orderDO == null) return null;
        return new OrderReadModel(
                orderDO.getId(), orderDO.getOrderNo(),
                orderDO.getBuyerId(), orderDO.getSellerId(),
                List.of(),
                orderDO.getTotalAmount(), orderDO.getStatus(),
                OrderStatus.getDescByCode(orderDO.getStatus()),
                orderDO.getPaymentStatus(), orderDO.getAddress(),
                orderDO.getPhone(), orderDO.getRemark(),
                orderDO.getCancelReason(), orderDO.getCancelTime(),
                orderDO.getCreateTime(), orderDO.getUpdateTime()
        );
    }

    /**
     * 携带行项的 ReadModel 重建（用于订单详情）。
     */
    default OrderReadModel toReadModel(OrderDO orderDO, List<OrderItemReadModel> items) {
        if (orderDO == null) return null;
        return new OrderReadModel(
                orderDO.getId(), orderDO.getOrderNo(),
                orderDO.getBuyerId(), orderDO.getSellerId(),
                items != null ? items : List.of(),
                orderDO.getTotalAmount(), orderDO.getStatus(),
                OrderStatus.getDescByCode(orderDO.getStatus()),
                orderDO.getPaymentStatus(), orderDO.getAddress(),
                orderDO.getPhone(), orderDO.getRemark(),
                orderDO.getCancelReason(), orderDO.getCancelTime(),
                orderDO.getCreateTime(), orderDO.getUpdateTime()
        );
    }

    // ==================== Item DO → Domain ====================

    default OrderItem toOrderItem(OrderItemDO itemDO) {
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

    default OrderItemReadModel toItemReadModel(OrderItemDO itemDO) {
        if (itemDO == null) return null;
        return new OrderItemReadModel(
                itemDO.getId(), itemDO.getProductId(),
                itemDO.getProductSnapshot(),
                itemDO.getUnitPrice(), itemDO.getQuantity(), itemDO.getSubtotal()
        );
    }

    // ==================== Aggregate → DO (Write path) ====================

    /**
     * OrderAggregate → OrderDO（不含行项）
     */
    default OrderDO toDataObject(OrderAggregate aggregate) {
        if (aggregate == null) return null;
        return OrderDO.builder()
                .id(aggregate.id().value())
                .orderNo(aggregate.orderNo().value())
                .buyerId(aggregate.buyerId().value())
                .sellerId(aggregate.sellerId().value())
                .totalAmount(aggregate.totalAmount().value())
                .status(aggregate.status().getCode())
                .paymentStatus(aggregate.paymentStatus().code())
                .address(aggregate.address().value())
                .phone(aggregate.phone().value())
                .remark(aggregate.remark())
                .cancelReason(aggregate.cancelReason())
                .cancelTime(aggregate.cancelTime())
                .build();
    }

    /**
     * OrderItem → OrderItemDO
     */
    default OrderItemDO toItemDO(String orderId, OrderItem item) {
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

    // ==================== JSON helpers ====================

    private static String toJson(ProductSnapshot snapshot) {
        try {
            return JACKSON.writeValueAsString(snapshot);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, "Failed to serialize ProductSnapshot", e);
        }
    }

    private static ProductSnapshot fromJson(String json) {
        try {
            return JACKSON.readValue(json, ProductSnapshot.class);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, "Failed to deserialize ProductSnapshot", e);
        }
    }
}
