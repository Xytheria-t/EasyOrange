package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

public record ProductSubmittedForReviewEvent(String productId, String operatorId, String sellerId,
                                              ProductStatus beforeStatus, ProductStatus afterStatus) implements ProductEvent {
}
