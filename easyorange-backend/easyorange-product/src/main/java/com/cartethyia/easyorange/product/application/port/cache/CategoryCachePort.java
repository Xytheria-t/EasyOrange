package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;

public interface CategoryCachePort {

    List<CategoryReadModel> getCategoriesByLevel(Integer level);

    List<CategoryReadModel> getCategoriesByParentId(String parentId);

    void evictByLevel(Integer level);

    void evictByParentId(String parentId);
}
