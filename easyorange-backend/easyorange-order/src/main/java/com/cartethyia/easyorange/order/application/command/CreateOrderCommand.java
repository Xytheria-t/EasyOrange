package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建订单命令。
 * <p>
 * record 载体替代 Lombok {@code @Data @Builder}，配合 Controller 端直接构造或静态工厂使用。
 * Jakarta Bean Validation 注解作用于 record 组件（字段级）。
 */
public record CreateOrderCommand(
        @NotEmpty(message = "订单项不能为空") @Size(max = 20, message = "单笔订单最多 20 件资产") @Valid
        List<CreateOrderItem> items,

        String address,
        String phone,
        String remark,
        String paymentMethod) {

    public record CreateOrderItem(
            @NotBlank(message = "资产 ID 不能为空") String productId,
            @Min(value = 1, message = "数量至少为 1") int quantity) {}
}
