package com.cartethyia.easyorange.order.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderCommand {

    private Long orderId;

    private String reason;
}
