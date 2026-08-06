package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import java.util.Optional;

public interface CategoryCachePort {

    List<CategoryReadModel> getCategoriesByLevel(Integer level);

    List<CategoryReadModel> getCategoriesByParentId(String parentId);

    Optional<CategoryReadModel> getCategoryById(String id);

    void evictAll();

    void evictByLevel(Integer level);

    void evictByParentId(String parentId);
}
