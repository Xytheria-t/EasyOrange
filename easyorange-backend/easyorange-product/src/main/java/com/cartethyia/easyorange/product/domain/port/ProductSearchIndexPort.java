package com.cartethyia.easyorange.product.domain.port;

public interface ProductSearchIndexPort {

    void indexProduct(Long productId);

    void updateProductIndex(Long productId);

    void removeProductIndex(Long productId);
}