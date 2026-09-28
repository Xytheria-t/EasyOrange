package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentDataMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentMapper;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.enums.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付仓储 — 写侧保存与读侧查询共用一个实现：本模块是单表状态机，读写形态无差异，
 * 另立读端口只会得到一对空转发方法；查询侧直接依赖 {@link PaymentRepository}（见 {@code PaymentQueryHandler}）。
 */
@Primary
@Repository
public class PaymentRepositoryImpl extends BaseRepository<PaymentMapper, PaymentDO> implements PaymentRepository {

    public PaymentRepositoryImpl(PaymentMapper paymentMapper) {
        super(paymentMapper);
    }

    @Override
    public void save(Payment aggregate) {
        mapper.insert(PaymentDataMapper.toPO(aggregate));
    }

    @Override
    public void update(Payment aggregate) {
        PaymentDO po = PaymentDataMapper.toPO(aggregate);
        int rows = mapper.updateById(po);

        if (rows == 0) {
            throw PaymentDomainException.of(
                    PaymentResultCode.PAYMENT_FAILED, "并发更新冲突，支付记录已被其他事务修改: paymentId=" + aggregate.id());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(PaymentDataMapper::toAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByPaymentNo(String paymentNo) {
        return Optional.ofNullable(
                        lambdaQuery().eq(PaymentDO::getPaymentNo, paymentNo).one())
                .map(PaymentDataMapper::toAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(String orderId) {
        return Optional.ofNullable(
                        lambdaQuery().eq(PaymentDO::getOrderId, orderId).one())
                .map(PaymentDataMapper::toAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, int pageNum, int pageSize) {
        // searchCount=false：本方法只返回 records，总数由调用方另走 countByUserIdAndStatus，
        // 不关掉会让分页查询额外多打一次全表 COUNT
        Page<PaymentDO> page = lambdaQuery()
                .eq(userId != null, PaymentDO::getUserId, userId)
                .eq(status != null, PaymentDO::getStatus, status)
                .orderByDesc(PaymentDO::getCreateTime)
                .page(new Page<>(pageNum, pageSize, false));
        return page.getRecords().stream().map(PaymentDataMapper::toAggregate).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndStatus(String userId, PaymentStatus status) {
        return lambdaQuery()
                .eq(userId != null, PaymentDO::getUserId, userId)
                .eq(status != null, PaymentDO::getStatus, status)
                .count();
    }
}
