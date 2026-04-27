package com.cartethyia.easyorange.payment.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cartethyia.easyorange.payment.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    void save(Payment payment);

    void update(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByStatus(Integer status);

    IPage<Payment> findPage(IPage<Payment> page, Long userId, Integer status);
}