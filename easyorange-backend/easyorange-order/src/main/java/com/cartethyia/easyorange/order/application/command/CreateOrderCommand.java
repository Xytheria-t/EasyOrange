package com.cartethyia.easyorange.order.application.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {

    @NotEmpty(message = "订单项不能为空")
    private List<CreateOrderItem> items;

    private String address;
    private String phone;
    private String remark;
    private Integer paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderItem {
        @NotNull(message = "资产 ID 不能为空")
        private Long productId;

        @Min(value = 1, message = "数量至少为 1")
        private int quantity = 1;
    }
}
