package com.cartethyia.easyorange.payment.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;

/**
 * 支付状态轻量视图 — 只含状态与支付方式描述，供轮询式状态查询使用。
 * <p>
 * 字段取值统一由 {@code PaymentViewAssembler} 映射，Controller 不自行拼装。
 */
public record PaymentStatusResponse(String status, String paymentMethod, LocalDateTime payTime) {}
