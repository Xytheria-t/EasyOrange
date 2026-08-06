package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 取消订单请求。
 * <p>
 * record 替代 Lombok {@code @Data}，配合 Spring 反序列化需要 {@code @JsonAlias} 或参数名匹配。
 * Spring Boot 4 + Jackson 3 默认使用 {@code ParameterNamesModule}，record 构造参数名与 JSON 字段名
 * 一致即可正确反序列化。
 */
public record CancelOrderRequest(
        @NotBlank(message = "取消原因不能为空") String reason) {}
