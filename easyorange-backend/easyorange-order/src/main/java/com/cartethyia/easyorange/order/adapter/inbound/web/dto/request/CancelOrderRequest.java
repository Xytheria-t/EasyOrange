package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 取消订单请求 — 请求 DTO 一律 record，Jackson 3 构造器绑定，无需额外注解。
 */
public record CancelOrderRequest(
        @NotBlank(message = "取消原因不能为空") String reason) {}
