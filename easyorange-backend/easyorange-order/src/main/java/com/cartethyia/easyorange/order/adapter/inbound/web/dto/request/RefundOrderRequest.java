package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 退款请求。
 * <p>
 * record 替代 Lombok {@code @Data}，Spring Boot 4 + Jackson 3 {@code ParameterNamesModule}
 * 根据构造函数参数名自动推断 JSON 字段名映射，无需额外注解。
 */
public record RefundOrderRequest(
        @NotBlank(message = "退款原因不能为空")
        String reason
) {}
