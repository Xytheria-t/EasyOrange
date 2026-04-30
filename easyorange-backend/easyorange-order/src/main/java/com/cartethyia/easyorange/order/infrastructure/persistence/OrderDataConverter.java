package com.cartethyia.easyorange.order.infrastructure.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderDataConverter {

    public OrderDO toDataObject(OrderAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return OrderDO.builder()
                .id(aggregate.id().value())
                .orderNo(aggregate.orderNo().value())
                .buyerId(aggregate.buyerId().value())
                .sellerId(aggregate.sellerId().value())
                .productId(aggregate.productId().value())
                .amount(aggregate.amount().amount())
                .status(aggregate.status().getCode())
                .paymentStatus(aggregate.paymentStatus())
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
                orderDO.getProductId(),
                orderDO.getAmount(),
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
                orderDO.getProductId(),
                orderDO.getAmount(),
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
}
