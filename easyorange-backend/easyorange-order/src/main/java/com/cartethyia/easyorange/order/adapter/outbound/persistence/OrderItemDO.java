package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("eo_order_item")
public class OrderItemDO extends BaseDO {
    private String id;
    private String orderId;
    private String productId;
    private String productSnapshot;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
