package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentDataMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentDO;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.port.PaymentQueryRepositoryPort;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisPaymentRepository extends BaseRepository<PaymentMapper, PaymentDO> implements PaymentRepositoryPort, PaymentQueryRepositoryPort {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final PaymentDataMapper paymentDataMapper;

    public MybatisPaymentRepository(PaymentMapper paymentMapper, PaymentDataMapper paymentDataMapper) {
        super(paymentMapper);
        this.paymentDataMapper = paymentDataMapper;
    }

    @Override
    public void save(PaymentAggregate aggregate) {
        mapper.insert(paymentDataMapper.toPO(aggregate));
    }

    @Override
    public void update(PaymentAggregate aggregate) {
        PaymentDO po = paymentDataMapper.toPO(aggregate);
        int rows = mapper.updateById(po);

        if (rows == 0) {
            throw PaymentDomainException.of(PaymentResultCode.PAYMENT_FAILED, "并发更新冲突，支付记录已被其他事务修改: paymentId=" + aggregate.id());
        }
    }

    @Override
    public Optional<PaymentAggregate> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(paymentDataMapper::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findByPaymentNo(String paymentNo) {
        return Optional.ofNullable(lambdaQuery().eq(PaymentDO::getPaymentNo, paymentNo).one())
                .map(paymentDataMapper::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findByOrderId(String orderId) {
        return Optional.ofNullable(lambdaQuery().eq(PaymentDO::getOrderId, orderId).one())
                .map(paymentDataMapper::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateById(String id) {
        return findById(id);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateByPaymentNo(String paymentNo) {
        return findByPaymentNo(paymentNo);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateByOrderId(String orderId) {
        return findByOrderId(orderId);
    }

    @Override
    public List<PaymentAggregate> findByUserIdAndStatus(String userId, String status, int pageNum, int pageSize) {
        Page<PaymentDO> page = lambdaQuery()
                .eq(userId != null, PaymentDO::getUserId, userId)
                .eq(status != null, PaymentDO::getStatus, status != null ? PaymentStatus.fromCode(status) : null)
                .orderByDesc(PaymentDO::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
        return page.getRecords().stream().map(paymentDataMapper::toAggregate).toList();
    }

    @Override
    public long countByUserIdAndStatus(String userId, String status) {
        return lambdaQuery()
                .eq(userId != null, PaymentDO::getUserId, userId)
                .eq(status != null, PaymentDO::getStatus, status != null ? PaymentStatus.fromCode(status) : null)
                .count();
    }
}