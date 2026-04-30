package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import com.cartethyia.easyorange.product.domain.valueobject.ProductStatusVO;
import com.cartethyia.easyorange.product.domain.valueobject.SellerId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    void save(Product product);

    void update(Product product);

    Optional<Product> findById(ProductId id);

    List<Product> findByIds(List<ProductId> ids);

    List<Product> findBySellerId(SellerId sellerId);

    void delete(ProductId id);

    boolean existsById(ProductId id);

    void updateStatus(ProductId id, ProductStatusVO status);
}
