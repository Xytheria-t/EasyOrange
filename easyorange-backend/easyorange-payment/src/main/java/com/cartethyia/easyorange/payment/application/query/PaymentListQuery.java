package com.cartethyia.easyorange.payment.application.query;

import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;

/**
 * 支付列表查询参数对象 — 收敛 getMyPayments/queryPayments 的散参为单一 record。
 * <p>
 * status 为 {@link PaymentStatus} 枚举（可 null 表示全部），由 Controller 层将
 * 前端 String code 转换为枚举，类型安全下沉到 application/domain 层。
 *
 * @param userId   用户 ID（getMyPayments 时由 service 层填充当前登录用户）
 * @param status   支付状态（可 null 表示全部）
 * @param pageNum  页码（null 或 < 1 时默认 1）
 * @param pageSize 每页大小（null 或 < 1 时默认 20）
 */
public record PaymentListQuery(
        String userId,
        PaymentStatus status,
        Integer pageNum,
        Integer pageSize
) {
    public PaymentListQuery {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
    }
}
