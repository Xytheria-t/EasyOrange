package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    // -- 写 --

    Product create(Product product);

    void update(Product product);

    void delete(ProductId id);

    // -- 读 --

    Optional<Product> findById(ProductId id);

    List<Product> findByIds(List<ProductId> ids);
}
