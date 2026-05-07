package com.cartethyia.easyorange.order.domain.port.output;

import com.cartethyia.easyorange.order.domain.aggregate.OrderAggregate;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(OrderAggregate aggregate);

    void update(OrderAggregate aggregate);

    Optional<OrderAggregate> findById(OrderId id);

    List<OrderAggregate> findByBuyerId(UserId buyerId);

    List<OrderAggregate> findBySellerId(UserId sellerId);

    List<OrderAggregate> findExpiredOrders(int timeoutMinutes);

    List<OrderAggregate> findByStatus(Integer status);

    List<OrderAggregate> findShippedOrdersBefore(LocalDateTime threshold);
}
