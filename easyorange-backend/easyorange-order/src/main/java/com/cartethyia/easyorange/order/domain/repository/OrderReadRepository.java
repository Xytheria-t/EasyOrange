package com.cartethyia.easyorange.order.domain.repository;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderReadRepository {

    Optional<OrderReadModel> findById(OrderId id);

    PageResult<OrderReadModel> findPage(OrderQueryCondition condition);

    long countByStatus(Integer status);

    List<OrderItemReadModel> findItemsByOrderId(String orderId);
}