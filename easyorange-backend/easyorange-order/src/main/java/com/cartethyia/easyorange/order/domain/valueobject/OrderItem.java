package com.cartethyia.easyorange.order.domain.valueobject;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.util.BizRequire;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record OrderItem(
        String id, ProductId productId, OrderItemSnapshot snapshot, Money unitPrice, int quantity, Money subtotal) {
    public OrderItem {
        BizRequire.notNull(snapshot, "订单项必须携带下单时的留痕快照");
        BizRequire.requireTrue(quantity > 0, "数量必须大于 0");
        BizRequire.requireTrue(subtotal.value().compareTo(BigDecimal.ZERO) > 0, "小计金额必须大于 0");
    }
}
