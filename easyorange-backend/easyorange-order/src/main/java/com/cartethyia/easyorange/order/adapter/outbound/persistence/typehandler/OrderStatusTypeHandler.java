package com.cartethyia.easyorange.order.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.order.domain.constant.OrderStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(OrderStatus.class)
@SuppressWarnings("unused")
public class OrderStatusTypeHandler extends CodeEnumTypeHandler<OrderStatus> {

    public OrderStatusTypeHandler() {
        super(OrderStatus::getCode, OrderStatus::fromCode);
    }
}
