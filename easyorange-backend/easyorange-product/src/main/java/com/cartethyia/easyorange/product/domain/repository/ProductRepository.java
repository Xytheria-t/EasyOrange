package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    void update(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findByIds(List<ProductId> ids);

    List<Product> findBySellerId(SellerId sellerId);

    void delete(ProductId id);

    boolean existsById(ProductId id);

    void updateStatus(ProductId id, ProductStatus status);
}
