package com.cartethyia.easyorange.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图对象 — 状态字段为 String code（与 {@code OrderStatus.code} 一致）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private String id;
    private String orderNo;
    private String buyerId;
    private String buyerUsername;
    private String sellerId;
    private String sellerUsername;
    private List<OrderItemVO> items;
    private BigDecimal totalAmount;
    private Boolean singleItem;
    private String status;
    private String statusDesc;
    private String address;
    private String phone;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemVO {
        private String itemId;
        private String productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
