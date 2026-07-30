package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    void delete(ProductId id);

    Optional<Product> findById(ProductId id);

    List<Product> findByIds(List<ProductId> ids);
}
