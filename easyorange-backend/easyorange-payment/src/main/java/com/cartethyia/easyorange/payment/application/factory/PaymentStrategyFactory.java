package com.cartethyia.easyorange.payment.application.factory;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.payment.domain.strategy.MockPaymentStrategy;
import com.cartethyia.easyorange.payment.domain.strategy.PaymentStrategy;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    private final MockPaymentStrategy mockPaymentStrategy;

    public PaymentStrategyFactory(MockPaymentStrategy mockPaymentStrategy) {
        this.mockPaymentStrategy = mockPaymentStrategy;
    }

    public PaymentStrategy getStrategy(Integer paymentMethod) {
        if (paymentMethod == null) {
            throw BusinessException.of("支付方式不能为空");
        }

        return switch (PaymentMethod.fromCode(paymentMethod)) {
            case WECHAT, ALIPAY, BALANCE -> mockPaymentStrategy;
            case null -> throw BusinessException.of("不支持的支付方式: " + paymentMethod);
        };
    }
}
