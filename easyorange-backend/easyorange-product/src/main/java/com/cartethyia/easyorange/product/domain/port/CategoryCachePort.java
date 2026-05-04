package com.cartethyia.easyorange.product.domain.port;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;

import java.util.List;
import java.util.Optional;

public interface CategoryCachePort {

    List<CategoryDO> getCategoriesByLevel(Integer level);

    List<CategoryDO> getCategoriesByParentId(Long parentId);

    Optional<CategoryDO> getCategoryById(Long id);

    void evictAll();

    void evictByLevel(Integer level);

    void evictByParentId(Long parentId);
}
