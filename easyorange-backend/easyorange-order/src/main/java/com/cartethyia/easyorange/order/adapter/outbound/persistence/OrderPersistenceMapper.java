package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderAggregate toAggregate(Order order) {
        if (order == null) {
            return null;
        }

        return OrderAggregate.fromEntity(order);
    }

    public Order toPersistence(OrderAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        Order order = new Order();
        order.setId(aggregate.getId());
        order.setOrderNo(aggregate.getOrderNo());
        order.setBuyerId(aggregate.getBuyerId());
        order.setSellerId(aggregate.getSellerId());
        order.setProductId(aggregate.getProductId());
        order.setAmount(aggregate.getAmount());
        order.setStatus(aggregate.getStatus());
        order.setPaymentStatus(aggregate.getPaymentStatus());
        order.setAddress(aggregate.getAddress());
        order.setPhone(aggregate.getPhone());
        order.setRemark(aggregate.getRemark());
        order.setCancelReason(aggregate.getCancelReason());
        order.setCancelTime(aggregate.getCancelTime());

        return order;
    }
}
