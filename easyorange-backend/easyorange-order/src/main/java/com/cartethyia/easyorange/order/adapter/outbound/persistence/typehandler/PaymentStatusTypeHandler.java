package com.cartethyia.easyorange.order.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.order.domain.valueobject.PaymentStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(PaymentStatus.class)
@SuppressWarnings("unused")
public class PaymentStatusTypeHandler extends CodeEnumTypeHandler<PaymentStatus> {

    public PaymentStatusTypeHandler() {
        super(PaymentStatus::getCode, PaymentStatus::fromCode);
    }
}
