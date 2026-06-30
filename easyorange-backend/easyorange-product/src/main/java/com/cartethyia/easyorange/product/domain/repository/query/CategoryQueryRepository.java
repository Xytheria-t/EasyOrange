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
}
