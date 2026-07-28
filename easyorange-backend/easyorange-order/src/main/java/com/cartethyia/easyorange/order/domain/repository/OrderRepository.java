package com.cartethyia.easyorange.order.domain.repository;

import com.cartethyia.easyorange.order.domain.aggregate.Order;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import com.cartethyia.easyorange.order.domain.valueobject.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order aggregate);

    void update(Order aggregate);

    Optional<Order> findById(OrderId id);

    List<Order> findExpiredOrders(int timeoutMinutes);

    List<Order> findShippedOrdersBefore(LocalDateTime threshold);

    List<OrderItem> findItemsByOrderId(String orderId);
}
