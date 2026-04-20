package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cartethyia.easyorange.payment.domain.repository.PaymentRepository;
import com.cartethyia.easyorange.payment.entity.Payment;
import com.cartethyia.easyorange.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisPaymentRepository implements PaymentRepository {

    private final PaymentMapper paymentMapper;

    @Override
    public void save(Payment payment) {
        paymentMapper.insert(payment);
    }

    @Override
    public void update(Payment payment) {
        paymentMapper.updateById(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(paymentMapper.selectById(id));
    }

    @Override
    public Optional<Payment> findByPaymentNo(String paymentNo) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getPaymentNo, paymentNo);
        return Optional.ofNullable(paymentMapper.selectOne(wrapper));
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        return Optional.ofNullable(paymentMapper.selectOne(wrapper));
    }

    @Override
    public List<Payment> findByUserId(Long userId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getUserId, userId);
        wrapper.orderByDesc(Payment::getCreateTime);
        return paymentMapper.selectList(wrapper);
    }

    @Override
    public List<Payment> findByStatus(Integer status) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getStatus, status);
        return paymentMapper.selectList(wrapper);
    }

    @Override
    public IPage<Payment> findPage(IPage<Payment> page, Long userId, Integer status) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Payment::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(Payment::getStatus, status);
        }
        wrapper.orderByDesc(Payment::getCreateTime);
        return paymentMapper.selectPage(page, wrapper);
    }
}