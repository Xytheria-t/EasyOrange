package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.valueobject.BuyerId;
import com.cartethyia.easyorange.order.domain.valueobject.CancellationReason;
import com.cartethyia.easyorange.order.domain.valueobject.OrderAmount;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderNo;
import com.cartethyia.easyorange.order.domain.valueobject.OrderProductId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderRemark;
import com.cartethyia.easyorange.order.domain.valueobject.OrderStatusVO;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatusVO;
import com.cartethyia.easyorange.order.domain.valueobject.SellerId;
import com.cartethyia.easyorange.order.domain.valueobject.ShippingContact;
import com.cartethyia.easyorange.order.domain.valueobject.Version;
import com.cartethyia.easyorange.order.entity.Order;
import com.cartethyia.easyorange.order.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderAggregate toAggregate(Order order) {
        if (order == null) {
            return null;
        }

        OrderId id = new OrderId(order.getId());
        OrderNo orderNo = new OrderNo(order.getOrderNo());
        BuyerId buyerId = new BuyerId(order.getBuyerId());
        SellerId sellerId = new SellerId(order.getSellerId());
        OrderProductId productId = new OrderProductId(order.getProductId());
        OrderAmount amount = new OrderAmount(order.getAmount());
        OrderStatusVO status = new OrderStatusVO(OrderStatus.fromCode(order.getStatus()));
        PaymentStatusVO paymentStatus = new PaymentStatusVO(order.getPaymentStatus() != null ? order.getPaymentStatus() : 0);
        ShippingContact shippingContact = new ShippingContact(order.getAddress(), order.getPhone());
        OrderRemark remark = new OrderRemark(order.getRemark());
        Version version = new Version(order.getVersion() != null ? order.getVersion() : 0);

        return OrderAggregate.load(
                id, orderNo, buyerId, sellerId, productId, amount,
                status, paymentStatus, shippingContact, remark, version
        );
    }

    public Order toPersistence(OrderAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        Order order = new Order();
        if (aggregate.getId() != null) {
            order.setId(aggregate.getId().value());
        }
        order.setOrderNo(aggregate.getOrderNo().value());
        order.setBuyerId(aggregate.getBuyerId().value());
        order.setSellerId(aggregate.getSellerId().value());
        order.setProductId(aggregate.getProductId().value());
        order.setAmount(aggregate.getAmount().value());
        order.setStatus(aggregate.getStatus().value().getCode());
        order.setPaymentStatus(aggregate.getPaymentStatus().value());
        if (aggregate.getShippingContact() != null) {
            order.setAddress(aggregate.getShippingContact().address());
            order.setPhone(aggregate.getShippingContact().phone());
        }
        if (aggregate.getRemark() != null) {
            order.setRemark(aggregate.getRemark().value());
        }
        order.setVersion(aggregate.getVersion().value());

        return order;
    }
}
