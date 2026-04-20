package com.cartethyia.easyorange.product.application.query;

import com.cartethyia.easyorange.common.cqrs.Query;
import com.cartethyia.easyorange.product.dto.vo.ProductVO;

public record ProductDetailQuery(Long productId) implements Query<ProductVO> {
}
