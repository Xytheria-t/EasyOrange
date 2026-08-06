package com.cartethyia.easyorange.product.domain.port;

public interface ProductSearchIndexPort {

    void indexProduct(String productId);

    void updateProductIndex(String productId);

    void removeProductIndex(String productId);
}
