package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentConverter;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.PaymentPO;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.OptimisticLockException;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentQueryRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisPaymentRepository extends BaseRepository<PaymentMapper, PaymentPO> implements PaymentRepositoryPort, PaymentQueryRepositoryPort {

    public MybatisPaymentRepository(PaymentMapper paymentMapper) {
        super(paymentMapper);
    }

    @Override
    public void save(PaymentAggregate aggregate) {
        mapper.insert(PaymentConverter.toPO(aggregate));
    }

    @Override
    public void update(PaymentAggregate aggregate) {
        PaymentPO po = PaymentConverter.toPO(aggregate);
        int rows = mapper.updateById(po);

        if (rows == 0) {
            throw OptimisticLockException.concurrentUpdate(aggregate.id());
        }
    }

    @Override
    public Optional<PaymentAggregate> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(PaymentConverter::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findByPaymentNo(String paymentNo) {
        return Optional.ofNullable(lambdaQuery().eq(PaymentPO::getPaymentNo, paymentNo).one())
                .map(PaymentConverter::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findByOrderId(Long orderId) {
        return Optional.ofNullable(lambdaQuery().eq(PaymentPO::getOrderId, orderId).one())
                .map(PaymentConverter::toAggregate);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateById(Long id) {
        return findById(id);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateByPaymentNo(String paymentNo) {
        return findByPaymentNo(paymentNo);
    }

    @Override
    public Optional<PaymentAggregate> findAggregateByOrderId(Long orderId) {
        return findByOrderId(orderId);
    }

    @Override
    public List<PaymentAggregate> findByUserIdAndStatus(Long userId, Integer status, int pageNum, int pageSize) {
        Page<PaymentPO> page = lambdaQuery()
                .eq(userId != null, PaymentPO::getUserId, userId)
                .eq(status != null, PaymentPO::getStatus, status)
                .orderByDesc(PaymentPO::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
        return page.getRecords().stream().map(PaymentConverter::toAggregate).toList();
    }

    @Override
    public long countByUserIdAndStatus(Long userId, Integer status) {
        return lambdaQuery()
                .eq(userId != null, PaymentPO::getUserId, userId)
                .eq(status != null, PaymentPO::getStatus, status)
                .count();
    }
}