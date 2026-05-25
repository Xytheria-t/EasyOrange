package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.Money;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import com.cartethyia.easyorange.order.domain.valueobject.ProductId;
import com.cartethyia.easyorange.order.domain.valueobject.ProductSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderDataConverter {

    private final ObjectMapper objectMapper;

    public OrderDataConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OrderDO toDataObject(OrderAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return OrderDO.builder()
                .id(aggregate.id().value())
                .orderNo(aggregate.orderNo().value())
                .buyerId(aggregate.buyerId().value())
                .sellerId(aggregate.sellerId().value())
                .totalAmount(aggregate.totalAmount().amount())
                .status(aggregate.status().getCode())
                .paymentStatus(aggregate.paymentStatus().code())
                .address(aggregate.address().value())
                .phone(aggregate.phone().value())
                .remark(aggregate.remark())
                .cancelReason(aggregate.cancelReason())
                .cancelTime(aggregate.cancelTime())
                .build();
    }

    public OrderAggregate toAggregate(OrderDO orderDO) {
        if (orderDO == null) {
            return null;
        }
        return OrderAggregate.fromRaw(
                orderDO.getId(),
                orderDO.getOrderNo(),
                orderDO.getBuyerId(),
                orderDO.getSellerId(),
                orderDO.getTotalAmount(),
                orderDO.getStatus(),
                orderDO.getPaymentStatus(),
                orderDO.getAddress(),
                orderDO.getPhone(),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime()
        );
    }

    public OrderAggregate toAggregate(OrderDO orderDO, List<OrderItem> items) {
        if (orderDO == null) {
            return null;
        }
        return OrderAggregate.fromRaw(
                orderDO.getId(),
                orderDO.getOrderNo(),
                orderDO.getBuyerId(),
                orderDO.getSellerId(),
                items,
                orderDO.getTotalAmount(),
                orderDO.getStatus(),
                orderDO.getPaymentStatus(),
                orderDO.getAddress(),
                orderDO.getPhone(),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime()
        );
    }

    public OrderReadModel toReadModel(OrderDO orderDO) {
        if (orderDO == null) {
            return null;
        }
        return new OrderReadModel(
                orderDO.getId(),
                orderDO.getOrderNo(),
                orderDO.getBuyerId(),
                orderDO.getSellerId(),
                List.of(),
                orderDO.getTotalAmount(),
                orderDO.getStatus(),
                OrderStatus.getDescByCode(orderDO.getStatus()),
                orderDO.getPaymentStatus(),
                orderDO.getAddress(),
                orderDO.getPhone(),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime(),
                orderDO.getCreateTime(),
                orderDO.getUpdateTime()
        );
    }

    public OrderReadModel toReadModel(OrderDO orderDO, List<OrderItemReadModel> items) {
        if (orderDO == null) {
            return null;
        }
        return new OrderReadModel(
                orderDO.getId(),
                orderDO.getOrderNo(),
                orderDO.getBuyerId(),
                orderDO.getSellerId(),
                items,
                orderDO.getTotalAmount(),
                orderDO.getStatus(),
                OrderStatus.getDescByCode(orderDO.getStatus()),
                orderDO.getPaymentStatus(),
                orderDO.getAddress(),
                orderDO.getPhone(),
                orderDO.getRemark(),
                orderDO.getCancelReason(),
                orderDO.getCancelTime(),
                orderDO.getCreateTime(),
                orderDO.getUpdateTime()
        );
    }

    public OrderItemDO toItemDO(Long orderId, OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderItemDO.builder()
                .id(item.id())
                .orderId(orderId)
                .productId(item.productId().value())
                .productSnapshot(toJson(item.snapshot()))
                .unitPrice(item.unitPrice().amount())
                .quantity(item.quantity())
                .subtotal(item.subtotal().amount())
                .build();
    }

    public OrderItemReadModel toItemReadModel(OrderItemDO itemDO) {
        if (itemDO == null) {
            return null;
        }
        return new OrderItemReadModel(
                itemDO.getId(),
                itemDO.getProductId(),
                itemDO.getProductSnapshot(),
                itemDO.getUnitPrice(),
                itemDO.getQuantity(),
                itemDO.getSubtotal()
        );
    }

    public OrderItem toOrderItem(OrderItemDO itemDO) {
        if (itemDO == null) {
            return null;
        }
        return OrderItem.builder()
                .id(itemDO.getId())
                .productId(ProductId.of(itemDO.getProductId()))
                .snapshot(fromJson(itemDO.getProductSnapshot()))
                .unitPrice(Money.of(itemDO.getUnitPrice()))
                .quantity(itemDO.getQuantity())
                .subtotal(Money.of(itemDO.getSubtotal()))
                .build();
    }

    private String toJson(ProductSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ProductSnapshot", e);
        }
    }

    private ProductSnapshot fromJson(String json) {
        try {
            return objectMapper.readValue(json, ProductSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize ProductSnapshot", e);
        }
    }
}
