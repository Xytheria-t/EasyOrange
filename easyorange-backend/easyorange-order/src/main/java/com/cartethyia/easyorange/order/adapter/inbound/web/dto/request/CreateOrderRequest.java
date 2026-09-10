package com.cartethyia.easyorange.order.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "订单项不能为空") @Valid List<OrderItemRequest> items,
        String address,

        @NotBlank(message = "联系电话不能为空") @Pattern(regexp = CommonConstant.PHONE_REGEX, message = "手机号格式不正确")
        String phone,

        String remark) {

    public record OrderItemRequest(
            @NotBlank(message = "资产 ID 不能为空") String productId,

            @NotNull(message = "数量不能为空") @Min(value = 1, message = "数量至少为 1")
            Integer quantity) {}
}
