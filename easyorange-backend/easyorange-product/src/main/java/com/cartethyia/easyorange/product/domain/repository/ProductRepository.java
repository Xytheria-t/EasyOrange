package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.entity.Product;

import java.util.List;

public interface ProductRepository {

    Product findById(Long id);

    List<Product> findByIds(List<Long> ids);

    List<Product> findByUserId(Long userId);

    boolean save(Product product);

    boolean update(Product product);

    void updateStock(Long productId, int delta);

    boolean removeById(Long id);

    boolean existsById(Long id);
}
