package com.cartethyia.easyorange.payment.application.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cartethyia.easyorange.common.dto.PageRequest;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.dto.request.QueryPaymentRequest;
import com.cartethyia.easyorange.payment.dto.vo.PaymentVO;
import com.cartethyia.easyorange.payment.entity.Payment;
import com.cartethyia.easyorange.payment.enums.PaymentMethod;
import com.cartethyia.easyorange.payment.enums.PaymentResultCode;
import com.cartethyia.easyorange.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentRepository paymentRepository;

    public PaymentVO getPaymentById(Long paymentId) {
        PaymentAggregate aggregate = paymentRepository.findById(paymentId)
                .map(PaymentAggregate::fromEntity)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return toPaymentVO(aggregate);
    }

    public PaymentVO getPaymentByOrderId(Long orderId) {
        PaymentAggregate aggregate = paymentRepository.findByOrderId(orderId)
                .map(PaymentAggregate::fromEntity)
                .orElseThrow(() -> BusinessException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        return toPaymentVO(aggregate);
    }

    public PageResult<PaymentVO> getMyPayments(QueryPaymentRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        PageRequest normalized = request.normalized();
        IPage<Payment> page = paymentRepository.findPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(normalized.getPageNum(), normalized.getPageSize()),
                userId,
                request.getStatus()
        );
        return toPageResult(page);
    }

    public PageResult<PaymentVO> queryPayments(QueryPaymentRequest request) {
        PageRequest normalized = request.normalized();
        IPage<Payment> page = paymentRepository.findPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(normalized.getPageNum(), normalized.getPageSize()),
                request.getUserId(),
                request.getStatus()
        );
        return toPageResult(page);
    }

    private PaymentVO toPaymentVO(PaymentAggregate aggregate) {
        return PaymentVO.builder()
                .id(aggregate.getId())
                .paymentNo(aggregate.getPaymentNo())
                .orderId(aggregate.getOrderId())
                .userId(aggregate.getUserId())
                .amount(aggregate.getAmount())
                .paymentMethod(aggregate.getPaymentMethod())
                .paymentMethodDesc(PaymentMethod.getDescByCode(aggregate.getPaymentMethod()))
                .status(aggregate.getStatus())
                .statusDesc(PaymentStatus.getDescByCode(aggregate.getStatus()))
                .transactionId(aggregate.getTransactionId())
                .refundReason(aggregate.getRefundReason())
                .refundTime(aggregate.getRefundTime())
                .createTime(aggregate.getCreateTime())
                .updateTime(aggregate.getUpdateTime())
                .build();
    }

    private PageResult<PaymentVO> toPageResult(IPage<Payment> page) {
        List<PaymentVO> voList = page.getRecords().stream()
                .map(PaymentAggregate::fromEntity)
                .map(this::toPaymentVO)
                .collect(Collectors.toList());
        return PageResult.fromIPage(page, voList);
    }
}
