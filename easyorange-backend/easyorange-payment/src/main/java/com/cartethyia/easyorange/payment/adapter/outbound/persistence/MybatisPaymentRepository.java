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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
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
            log.error("乐观锁冲突: paymentId={}, version={}", aggregate.id(), aggregate.version());
            throw OptimisticLockException.concurrentUpdate(aggregate.id());
        }
    }

    @Override
    public Optional<PaymentAggregate> findById(Long id) {
        PaymentPO po = mapper.selectById(id);
        return Optional.ofNullable(PaymentConverter.toAggregate(po));
    }

    @Override
    public Optional<PaymentAggregate> findByPaymentNo(String paymentNo) {
        PaymentPO po = lambdaQuery()
                .eq(PaymentPO::getPaymentNo, paymentNo)
                .one();
        return Optional.ofNullable(PaymentConverter.toAggregate(po));
    }

    @Override
    public Optional<PaymentAggregate> findByOrderId(Long orderId) {
        PaymentPO po = lambdaQuery()
                .eq(PaymentPO::getOrderId, orderId)
                .one();
        return Optional.ofNullable(PaymentConverter.toAggregate(po));
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
        var query = lambdaQuery();
        if (userId != null) {
            query.eq(PaymentPO::getUserId, userId);
        }
        if (status != null) {
            query.eq(PaymentPO::getStatus, status);
        }
        query.orderByDesc(PaymentPO::getCreateTime);
        Page<PaymentPO> page = query.page(new Page<>(pageNum, pageSize));
        return page.getRecords().stream()
                .map(PaymentConverter::toAggregate)
                .toList();
    }

    @Override
    public long countByUserIdAndStatus(Long userId, Integer status) {
        var query = lambdaQuery();
        if (userId != null) {
            query.eq(PaymentPO::getUserId, userId);
        }
        if (status != null) {
            query.eq(PaymentPO::getStatus, status);
        }
        return query.count();
    }
}