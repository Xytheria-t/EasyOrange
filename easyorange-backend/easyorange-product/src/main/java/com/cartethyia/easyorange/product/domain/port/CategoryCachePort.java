package com.cartethyia.easyorange.product.domain.port;

import java.util.List;
import java.util.Optional;

public interface CategoryCachePort<T> {

    List<T> getCategoriesByLevel(Integer level);

    List<T> getCategoriesByParentId(Long parentId);

    Optional<T> getCategoryById(Long id);

    void evictAll();

    void evictByLevel(Integer level);

    void evictByParentId(Long parentId);
}
