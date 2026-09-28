package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付查询处理器 — CQRS Read 侧应用服务。
 * <p>
 * 直接依赖写仓储 {@link PaymentRepository}：本模块是单表状态机，读写形态无差异，
 * 另立一个只做空转发的读端口属于名义 CQRS，不如承认「本模块没有独立读模型」。
 * 读写隔离落在事务语义上——此处一律 {@code readOnly = true}，写路径的
 * {@code rollbackFor = Exception.class} 不下沉到查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Payment getPaymentById(String paymentId, String operatorId) {
        return assertOwnership(paymentRepository.findById(paymentId), operatorId);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(String orderId, String operatorId) {
        return assertOwnership(paymentRepository.findByOrderId(orderId), operatorId);
    }

    /**
     * 我的支付记录 — userId 由 Web 边界解析，**以入参为准**而不是 query 里那个：
     * 「我的」的可信来源只有 SecurityContext 解析出的 id，query 对象不该有机会指定查谁的记录
     */
    @Transactional(readOnly = true)
    public PageResult<Payment> getMyPayments(String userId, PaymentListQuery query) {
        return queryPaymentsInternal(new PaymentListQuery(userId, query.status(), query.pageNum(), query.pageSize()));
    }

    /**
     * 通用支付记录查询（管理端） — 通过 PaymentListQuery 收敛参数。
     */
    @Transactional(readOnly = true)
    public PageResult<Payment> queryPayments(PaymentListQuery query) {
        return queryPaymentsInternal(query);
    }

    /**
     * 资源归属校验（越权防护）— 查询者必须与支付单所属用户一致，
     * 不一致时按「记录不存在」处理（B4001，B 段前缀统一映射 400），避免泄露支付单存在性。
     */
    private Payment assertOwnership(Optional<Payment> found, String operatorId) {
        Payment payment = found.orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        if (!payment.userId().equals(operatorId)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND);
        }
        return payment;
    }

    /** 分页参数已由 {@link PaymentListQuery} 紧凑构造器收敛为非空且带上限，此处不再二次兜底。 */
    private PageResult<Payment> queryPaymentsInternal(PaymentListQuery query) {
        List<Payment> aggregates = paymentRepository.findByUserIdAndStatus(
                query.userId(), query.status(), query.pageNum(), query.pageSize());
        long total = paymentRepository.countByUserIdAndStatus(query.userId(), query.status());
        return PageResult.of(aggregates, total, query.pageNum(), query.pageSize());
    }
}
