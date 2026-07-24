package com.cartethyia.easyorange.payment.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(PaymentStatus.class)
@SuppressWarnings("unused")
public class PaymentStatusTypeHandler extends CodeEnumTypeHandler<PaymentStatus> {

    public PaymentStatusTypeHandler() {
        super(PaymentStatus::getCode, PaymentStatus::fromCode);
    }
}
