package com.cartethyia.easyorange.order.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderItemReadModel;
import com.cartethyia.easyorange.order.application.query.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import com.cartethyia.easyorange.order.domain.port.OrderQueryCondition;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderQueryRepository {

    Optional<OrderReadModel> findById(OrderId id);

    PageResult<OrderReadModel> findPage(OrderQueryCondition condition);

    /**
     * 按状态统计订单数。
     *
     * @param status 订单状态，null 表示统计全部
     */
    long countByStatus(OrderStatus status);

    /**
     * 统计创建时间不早于指定时刻的订单数（全部状态）。
     */
    long countByCreatedAfter(LocalDateTime since);

    List<OrderItemReadModel> findItemsByOrderId(String orderId);
}
