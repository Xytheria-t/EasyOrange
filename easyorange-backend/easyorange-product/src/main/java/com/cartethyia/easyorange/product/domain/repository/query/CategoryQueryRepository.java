package com.cartethyia.easyorange.product.domain.repository.query;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;

import java.util.List;
import java.util.Map;

public interface CategoryQueryRepository {

    List<CategoryDO> findByParentId(String parentId);

    List<CategoryDO> findByLevel(Integer level);

    CategoryDO findByName(String name);

    List<CategoryDO> findByIds(List<String> ids);

    Map<String, Long> countProductsByCategoryIds(List<String> categoryIds);

    /**
     * 统计指定分类（含子分类）下的在售商品数量，子分类商品计数归到父分类。
     */
    Map<String, Long> countProductsByCategoryIdsWithChildren(List<String> categoryIds);
}
