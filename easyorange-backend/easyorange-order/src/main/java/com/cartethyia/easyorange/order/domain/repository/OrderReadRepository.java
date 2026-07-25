package com.cartethyia.easyorange.order.domain.repository;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderReadRepository {

    Optional<OrderReadModel> findById(OrderId id);

    PageResult<OrderReadModel> findPage(OrderQueryCondition condition);

    /**
     * 按状态统计订单数。
     *
     * @param status 订单状态，null 表示统计全部
     */
    long countByStatus(OrderStatus status);

    List<OrderItemReadModel> findItemsByOrderId(String orderId);
}
