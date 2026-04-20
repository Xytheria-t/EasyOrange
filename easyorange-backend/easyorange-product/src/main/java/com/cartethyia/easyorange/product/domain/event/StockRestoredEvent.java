package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class StockRestoredEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Product";

    private Long productId;
    @EqualsAndHashCode.Exclude
    private Integer quantity = 1;

    public StockRestoredEvent(Long productId) {
        super(AGGREGATE_TYPE);
        this.productId = productId;
        this.quantity = 1;
    }
}
