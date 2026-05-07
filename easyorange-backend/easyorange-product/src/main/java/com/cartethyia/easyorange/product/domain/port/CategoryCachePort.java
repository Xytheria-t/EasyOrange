package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;

import java.util.List;
import java.util.Optional;

public interface CategoryCachePort {

    List<CategoryReadModel> getCategoriesByLevel(Integer level);

    List<CategoryReadModel> getCategoriesByParentId(Long parentId);

    Optional<CategoryReadModel> getCategoryById(Long id);

    void evictAll();

    void evictByLevel(Integer level);

    void evictByParentId(Long parentId);
}
