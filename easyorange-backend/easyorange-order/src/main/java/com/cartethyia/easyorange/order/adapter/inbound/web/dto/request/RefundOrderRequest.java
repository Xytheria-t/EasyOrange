package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 退款请求 — 请求 DTO 一律 record，Jackson 3 构造器绑定，无需额外注解。
 */
public record RefundOrderRequest(
        @NotBlank(message = "退款原因不能为空") String reason) {}
