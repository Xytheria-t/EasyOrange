package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.payment.application.metrics.PaymentMetricsService;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付命令处理器 — CQRS Write 侧应用服务，收口支付全部用例（创建/支付/退款/关闭）。
 * <p>
 * 支付与退款走「准备 → 网关 → 确认」顺序两阶段（单数据库场景下遵循 ADR-0007「拒绝 Saga」：
 * 本地事务提供原子性，外部网关调用无法纳入同一事务）。每个 phase 的事务边界在
 * {@link PaymentPhaseExecutor}（独立 Bean，Spring 代理生效），编排方法自身不持有事务，
 * 避免事务跨网关调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandHandler {

    private static final String PAY_LOCK_PREFIX = "payment:lock:pay:";
    private static final String REFUND_LOCK_PREFIX = "payment:lock:refund:";
    /**
     * 锁等待 0 秒：并发重复回调立即失败返回 429（可重试），由网关重试兜底；
     * 不排队等待，避免回调线程挂在可能长时间运行的网关调用之后。
     */
    private static final long LOCK_TRY_TIMEOUT_SECONDS = 0;

    private final PaymentRepositoryPort paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final IdGenerator idGenerator;
    private final DistributedLockPort lockPort;
    private final PaymentMetricsService metricsService;
    private final PaymentPhaseExecutor phaseExecutor;

    @Transactional(rollbackFor = Exception.class)
    public String handle(String userId, CreatePaymentCommand command) {
        String paymentId = idGenerator.generateId();
        var spec = new PaymentCreateSpec(
                paymentId,
                command.orderId(),
                userId,
                command.amount(),
                PaymentMethod.fromCode(command.paymentMethod()),
                command.attach());
        Transition<Payment, PaymentCreatedEvent> result = Payment.create(spec);

        paymentRepository.save(result.aggregate());
        domainEventPublisher.publish(result.event());

        return result.aggregate().id();
    }

    /**
     * 支付：两阶段（本地事务 + 外部网关）。
     * <p>
     * 单数据库场景下遵循 ADR-0007「拒绝 Saga」——本地事务提供原子性，
     * 外部网关调用无法纳入同一事务，因此用「准备 → 网关 → 确认」顺序两阶段，
     * 网关失败时回退状态，无需跨服务编排。
     */
    public void handle(PayCommand command) {
        String lockKey = PAY_LOCK_PREFIX + command.paymentNo();

        executeWithLock(lockKey, () -> {
            String paymentId = phaseExecutor.preparePayPhase1(command.paymentNo());
            PaymentResult payResult = phaseExecutor.invokePayGateway(paymentId);
            if (payResult.isSuccess()) {
                phaseExecutor.confirmPayPhase2(paymentId, payResult);
            } else {
                phaseExecutor.rollbackPayStatus(paymentId);
            }
        });
    }

    /**
     * 退款：两阶段（本地事务 + 外部网关），与支付一致遵循 ADR-0007 拒绝 Saga。
     */
    public void handle(RefundPaymentCommand command) {
        String lockKey = REFUND_LOCK_PREFIX + command.paymentId();

        executeWithLock(lockKey, () -> {
            BigDecimal refundAmount = command.refundAmount();
            String paymentId = command.paymentId();

            phaseExecutor.prepareRefundPhase1(paymentId, refundAmount);
            RefundResult refundResult = phaseExecutor.invokeRefundGateway(paymentId, refundAmount);
            if (refundResult.isSuccess()) {
                phaseExecutor.confirmRefundPhase2(paymentId, refundResult, refundAmount);
            } else {
                phaseExecutor.rollbackRefundStatus(paymentId);
            }
        });
    }

    /**
     * 关闭支付。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(ClosePaymentCommand command) {
        Payment aggregate = paymentRepository
                .findById(command.paymentId())
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));

        var result = aggregate.close();
        paymentRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    /**
     * 带锁执行并记录并发冲突指标 — 锁获取失败由用例层记录，指标属支付域而不属锁基础设施。
     * <p>
     * 锁争用映射为支付域 {@link PaymentResultCode#PAYMENT_BUSY}（A0429 → 429，可重试语义），
     * 而非把基础设施异常直接上抛落入 500 兜底；原异常带锁 key 记 warn 日志供运维定位。
     */
    private void executeWithLock(String lockKey, Runnable operation) {
        try {
            lockPort.executeWithLock(lockKey, LOCK_TRY_TIMEOUT_SECONDS, operation);
        } catch (LockAcquisitionException e) {
            metricsService.recordConcurrentConflict();
            log.warn("支付处理锁争用, key={}", lockKey, e);
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_BUSY);
        }
    }
}
