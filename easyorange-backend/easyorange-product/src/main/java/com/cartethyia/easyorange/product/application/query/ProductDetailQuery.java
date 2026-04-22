package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.cqrs.Query;

public record ProductDetailQuery(Long productId) implements Query {
}