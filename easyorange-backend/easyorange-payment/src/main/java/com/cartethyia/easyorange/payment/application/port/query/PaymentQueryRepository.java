package com.cartethyia.easyorange.payment.application.port.query;

import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentQueryRepository {

    Optional<Payment> findAggregateById(String id);

    Optional<Payment> findAggregateByOrderId(String orderId);

    /**
     * 按用户 ID 和支付状态分页查询。
     *
     * @param userId   用户 ID（可 null 表示不限制）
     * @param status   支付状态（可 null 表示全部）
     * @param pageNum  页码
     * @param pageSize 每页大小
     */
    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, int pageNum, int pageSize);

    /**
     * 按用户 ID 和支付状态计数。
     *
     * @param userId 用户 ID（可 null 表示不限制）
     * @param status 支付状态（可 null 表示全部）
     */
    long countByUserIdAndStatus(String userId, PaymentStatus status);
}
