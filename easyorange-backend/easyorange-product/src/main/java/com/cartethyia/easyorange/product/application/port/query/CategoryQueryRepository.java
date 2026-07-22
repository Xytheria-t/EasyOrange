package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;

import java.util.List;
import java.util.Map;

public interface CategoryQueryRepository {

    List<CategoryReadModel> findByParentId(String parentId);

    List<CategoryReadModel> findByLevel(Integer level);

    CategoryReadModel findByName(String name);

    List<CategoryReadModel> findByIds(List<String> ids);

    Map<String, Long> countProductsByCategoryIds(List<String> categoryIds);

    /**
     * 统计指定分类（含子分类）下的在售商品数量，子分类商品计数归到父分类。
     */
    Map<String, Long> countProductsByCategoryIdsWithChildren(List<String> categoryIds);
}
