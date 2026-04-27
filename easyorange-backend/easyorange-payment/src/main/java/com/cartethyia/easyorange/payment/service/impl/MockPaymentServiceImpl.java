package com.cartethyia.easyorange.payment.service.impl;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.payment.constant.PaymentConstants;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.dto.request.MockPaymentRequest;
import com.cartethyia.easyorange.payment.dto.vo.PaymentVO;
import com.cartethyia.easyorange.payment.entity.Payment;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import com.cartethyia.easyorange.payment.service.MockPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements MockPaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public PaymentVO createMockPayment(Long orderId, Integer paymentMethod, BigDecimal amount) {
        Payment payment = Payment.builder()
                .paymentNo(PaymentConstants.MOCK_PAYMENT_NO_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING.getCode())
                .build();
        paymentRepository.save(payment);

        return PaymentVO.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .orderId(orderId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentMethodDesc(PaymentMethod.getDescByCode(paymentMethod))
                .status(PaymentStatus.PENDING.getCode())
                .statusDesc(PaymentStatus.getDescByCode(PaymentStatus.PENDING.getCode()))
                .createTime(payment.getCreateTime())
                .build();
    }

    @Override
    public PaymentVO processMockPayment(MockPaymentRequest request) {
        Payment payment = getPaymentOrThrow(request.getPaymentId());

        if (Boolean.TRUE.equals(request.getSuccess())) {
            payment.setStatus(PaymentStatus.SUCCESS.getCode());
            payment.setTransactionId(PaymentConstants.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        } else {
            payment.setStatus(PaymentStatus.FAILED.getCode());
        }
        paymentRepository.update(payment);

        return buildPaymentVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO mockPaymentSuccess(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS.getCode());
        payment.setTransactionId(PaymentConstants.MOCK_TXN_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        paymentRepository.update(payment);

        return buildPaymentVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO mockPaymentFail(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        payment.setStatus(PaymentStatus.FAILED.getCode());
        paymentRepository.update(payment);

        return buildPaymentVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockRefund(Long paymentId, String reason) {
        Payment payment = getPaymentOrThrow(paymentId);
        BizRequire.isTrue(PaymentStatus.SUCCESS.getCode().equals(payment.getStatus()), PaymentResultCode.PAYMENT_INVALID_STATUS);
        payment.setStatus(PaymentStatus.REFUNDED.getCode());
        payment.setRefundReason(reason);
        payment.setRefundTime(LocalDateTime.now());
        paymentRepository.update(payment);
    }

    private Payment getPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
    }

    private PaymentVO buildPaymentVO(Payment payment) {
        return PaymentVO.builder()
                .id(payment.getId())
                .paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentMethodDesc(PaymentMethod.getDescByCode(payment.getPaymentMethod()))
                .status(payment.getStatus())
                .statusDesc(PaymentStatus.getDescByCode(payment.getStatus()))
                .transactionId(payment.getTransactionId())
                .createTime(payment.getCreateTime())
                .build();
    }
}
