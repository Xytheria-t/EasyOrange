package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class ProductDeletedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Product";

    private Long productId;
    private Long userId;

    public ProductDeletedEvent(Long productId, Long userId) {
        super(AGGREGATE_TYPE);
        this.productId = productId;
        this.userId = userId;
    }
}
