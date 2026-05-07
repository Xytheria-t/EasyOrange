package com.cartethyia.easyorange.order.domain.port.output;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.order.domain.readmodel.OrderReadModel;
import com.cartethyia.easyorange.order.domain.valueobject.OrderId;

import java.util.Optional;

public interface OrderReadRepository {

    Optional<OrderReadModel> findById(OrderId id);

    PageResult<OrderReadModel> findPage(OrderQueryCondition condition);

    long countByStatus(Integer status);
}
