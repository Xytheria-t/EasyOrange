package com.cartethyia.easyorange.payment.application.command;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.lock.DistributedLockPort;
import com.cartethyia.easyorange.framework.lock.LockAcquisitionException;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentCreateSpec;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.event.PaymentCreatedEvent;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentResult;
import com.cartethyia.easyorange.payment.domain.port.RefundResult;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付命令处理器 — CQRS Write 侧应用服务，收口支付全部用例（创建/支付/回调确认/退款/关闭）。
 * <p>
 * 支付与退款走「准备 → 网关 → 确认」顺序两阶段（单数据库场景下遵循 ADR-0007「拒绝 Saga」：
 * 本地事务提供原子性，外部网关调用无法纳入同一事务）。每个 phase 的事务边界在
 * {@link PaymentPhaseExecutor}（独立 Bean，Spring 代理生效），编排方法自身不持有事务，
 * 避免事务跨网关调用。回调确认是例外：扣款已在渠道侧完成，无需调用网关，
 * 直接以回调携带的 transactionId 走「准备 → 确认」两步。
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

    private final PaymentRepository paymentRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final IdGenerator idGenerator;
    private final DistributedLockPort lockPort;
    private final MeterRegistry meterRegistry;
    private final PaymentPhaseExecutor phaseExecutor;

    @Transactional(rollbackFor = Exception.class)
    public String createPayment(String userId, CreatePaymentCommand command) {
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
    public void pay(PayCommand command) {
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
     * 支付回调确认：扣款已在渠道侧完成，直接以回调携带的 transactionId 确认成功。
     * <p>
     * 与 {@link #pay(PayCommand)} 不同——不调用支付网关（回调本身就是网关的结果通知），
     * 仅「准备 → 确认」两步；回调金额非空时先校验与支付单一致（防止金额被篡改，
     * HMAC 签名只覆盖 paymentNo|transactionId）。
     */
    public void processCallback(PaymentCallbackCommand command) {
        String lockKey = PAY_LOCK_PREFIX + command.paymentNo();

        executeWithLock(lockKey, () -> {
            verifyCallbackAmount(command);
            String paymentId = phaseExecutor.preparePayPhase1(command.paymentNo());
            phaseExecutor.confirmPayPhase2(paymentId, PaymentResult.success(command.transactionId()));
        });
    }

    /**
     * 退款：两阶段（本地事务 + 外部网关），与支付一致遵循 ADR-0007 拒绝 Saga。
     * <p>
     * 操作者必须与支付单所属用户一致（越权防护，见 {@link #assertOwnership}）。
     */
    public void refundPayment(RefundPaymentCommand command) {
        String lockKey = REFUND_LOCK_PREFIX + command.paymentId();

        executeWithLock(lockKey, () -> {
            assertOwnership(command.paymentId(), command.userId());
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
    public void closePayment(ClosePaymentCommand command) {
        Payment aggregate = assertOwnership(command.paymentId(), command.userId());

        var result = aggregate.close();
        paymentRepository.update(result.aggregate());
        domainEventPublisher.publish(result.event());
    }

    // ==================== 订单侧入口（以 orderId 为键） ====================

    /**
     * 按订单 ID 发起支付 — 订单模块的 {@code PaymentGatewayPort.pay(orderId)} 以订单 ID 为键，
     * 而 {@link PayCommand} 以支付单号为键，解析步骤收口在此，调用方不必接触支付仓储。
     * <p>
     * 不持有事务：委托 {@link #pay(PayCommand)} 走两阶段，事务边界在 {@link PaymentPhaseExecutor}。
     *
     * @throws PaymentDomainException 支付单不存在（B4001）
     */
    public void payByOrderId(String orderId) {
        pay(new PayCommand(resolveByOrderId(orderId).paymentNo(), null, null));
    }

    /**
     * 按订单 ID 退款 — 操作者记为支付单所属用户（通过归属校验），供订单取消等系统内部路径使用。
     *
     * @throws PaymentDomainException 支付单不存在（B4001）
     */
    public void refundByOrderId(String orderId, String reason) {
        Payment payment = resolveByOrderId(orderId);
        refundPayment(new RefundPaymentCommand(payment.id(), payment.userId(), payment.amount(), reason));
    }

    /**
     * 按订单 ID 解析支付单 — 订单与支付单在同一下单事务内落库，正常路径下必然存在；
     * 缺失属数据不一致，按 B4001 显性失败而非静默跳过（静默会让用户点了支付却毫无反馈）。
     */
    private Payment resolveByOrderId(String orderId) {
        return paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> PaymentDomainException.notFound("orderId=" + orderId));
    }

    /**
     * 回调金额校验 — 回调未携带金额时跳过（签名已覆盖 paymentNo|transactionId）。
     * <p>
     * 金额不一致视为篡改，按业务异常拒绝（不落 500 兜底）。
     */
    private void verifyCallbackAmount(PaymentCallbackCommand command) {
        if (command.amount() == null) {
            return;
        }
        Payment aggregate = paymentRepository
                .findByPaymentNo(command.paymentNo())
                .orElseThrow(() -> PaymentDomainException.notFound("paymentNo=" + command.paymentNo()));
        if (aggregate.amount().compareTo(command.amount()) != 0) {
            throw PaymentDomainException.of(
                    PaymentResultCode.CALLBACK_AMOUNT_MISMATCH, "回调金额与支付单金额不一致: paymentNo=" + command.paymentNo());
        }
    }

    /**
     * 资源归属校验（越权防护）— 操作者必须与支付单所属用户一致。
     * <p>
     * 不一致时按「记录不存在」处理（B4001，B 段前缀统一映射 400），避免向调用方泄露支付单存在性。
     */
    private Payment assertOwnership(String paymentId, String operatorId) {
        Payment aggregate = paymentRepository
                .findById(paymentId)
                .orElseThrow(() -> PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND));
        if (!aggregate.userId().equals(operatorId)) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_NOT_FOUND);
        }
        return aggregate;
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
            Counter.builder("payment.concurrent.conflict.total")
                    .description("Total number of concurrent payment conflicts")
                    .tag("type", "concurrency")
                    .register(meterRegistry)
                    .increment();
            log.warn("支付处理锁争用, key={}", lockKey, e);
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_BUSY);
        }
    }
}
