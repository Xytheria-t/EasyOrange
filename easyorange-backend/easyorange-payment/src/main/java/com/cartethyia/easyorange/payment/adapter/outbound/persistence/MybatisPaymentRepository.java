package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.converter.PaymentConverter;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.PaymentPO;
import com.cartethyia.easyorange.payment.domain.aggregate.PaymentAggregate;
import com.cartethyia.easyorange.payment.domain.exception.OptimisticLockException;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentQueryRepositoryPort;
import com.cartethyia.easyorange.payment.domain.port.output.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MybatisPaymentRepository implements PaymentRepositoryPort, PaymentQueryRepositoryPort {

    private final PaymentMapper paymentMapper;

    @Override
    public void save(PaymentAggregate aggregate) {
        paymentMapper.insert(PaymentConverter.toPO(aggregate));
    }

    @Override
    public void update(PaymentAggregate aggregate) {
        PaymentPO po = PaymentConverter.toPO(aggregate);
        int rows = paymentMapper.updateById(po);
        
        if (rows == 0) {
            log.error("乐观锁冲突: paymentId={}, version={}", aggregate.id(), aggregate.version());
            throw OptimisticLockException.concurrentUpdate(aggregate.id());
        }
    }

    @Override
    public Optional<PaymentAggregate> findById(Long id) {
        PaymentPO po = paymentMapper.selectById(id);
        return Optional.ofNullable(PaymentConverter.toAggregate(po));
    }

    @Override
    public Optional<PaymentAggregate> findByPaymentNo(String paymentNo) {
        LambdaQueryWrapper<PaymentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentPO::getPaymentNo, paymentNo);
        PaymentPO po = paymentMapper.selectOne(wrapper);
        return Optional.ofNullable(PaymentConverter.toAggregate(po));
    }

    @Override
    public Optional<PaymentAggregate> findByOrderId(Long orderId) {
        LambdaQueryWrapper<PaymentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentPO::getOrderId, orderId);
        PaymentPO po = paymentMapper.selectOne(wrapper);
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
        LambdaQueryWrapper<PaymentPO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PaymentPO::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(PaymentPO::getStatus, status);
        }
        wrapper.orderByDesc(PaymentPO::getCreateTime);
        Page<PaymentPO> page = paymentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream()
                .map(PaymentConverter::toAggregate)
                .toList();
    }

    @Override
    public long countByUserIdAndStatus(Long userId, Integer status) {
        LambdaQueryWrapper<PaymentPO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(PaymentPO::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(PaymentPO::getStatus, status);
        }
        return paymentMapper.selectCount(wrapper);
    }
}
