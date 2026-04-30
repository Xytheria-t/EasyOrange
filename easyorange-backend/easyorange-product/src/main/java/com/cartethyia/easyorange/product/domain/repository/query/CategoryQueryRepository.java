package com.cartethyia.easyorange.product.domain.repository.query;

import com.cartethyia.easyorange.product.infrastructure.persistence.dataobject.CategoryDO;

import java.util.List;

public interface CategoryQueryRepository {

    List<CategoryDO> findByParentId(Long parentId);

    List<CategoryDO> findByLevel(Integer level);

    CategoryDO findByName(String name);

    List<CategoryDO> findByIds(List<Long> ids);
}