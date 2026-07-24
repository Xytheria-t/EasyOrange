package com.cartethyia.easyorange.payment.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(PaymentMethod.class)
@SuppressWarnings("unused")
public class PaymentMethodTypeHandler extends CodeEnumTypeHandler<PaymentMethod> {

    public PaymentMethodTypeHandler() {
        super(PaymentMethod::getCode, PaymentMethod::fromCode);
    }
}
