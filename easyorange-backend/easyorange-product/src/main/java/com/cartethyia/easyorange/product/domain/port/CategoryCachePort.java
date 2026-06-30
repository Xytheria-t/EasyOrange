package com.cartethyia.easyorange.product.domain.port;

import java.util.List;
import java.util.Optional;

public interface CategoryCachePort<T> {

    List<T> getCategoriesByLevel(Integer level);

    List<T> getCategoriesByParentId(String parentId);

    Optional<T> getCategoryById(String id);

    void evictAll();

    void evictByLevel(Integer level);

    void evictByParentId(String parentId);
}
