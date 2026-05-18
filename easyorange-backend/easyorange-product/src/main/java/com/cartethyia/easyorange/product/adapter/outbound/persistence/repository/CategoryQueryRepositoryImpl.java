package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryProductCountDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CategoryQueryRepositoryImpl extends BaseRepository<CategoryMapper, CategoryDO> implements CategoryQueryRepository {

    public CategoryQueryRepositoryImpl(CategoryMapper categoryMapper) {
        super(categoryMapper);
    }

    @Override
    public List<CategoryDO> findByParentId(Long parentId) {
        return lambdaQuery()
                .eq(CategoryDO::getParentId, parentId)
                .orderByAsc(CategoryDO::getSortOrder)
                .list();
    }

    @Override
    public List<CategoryDO> findByLevel(Integer level) {
        return lambdaQuery()
                .eq(CategoryDO::getLevel, level)
                .orderByAsc(CategoryDO::getSortOrder)
                .list();
    }

    @Override
    public CategoryDO findByName(String name) {
        return lambdaQuery()
                .eq(CategoryDO::getName, name)
                .one();
    }

    @Override
    public List<CategoryDO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }

    @Override
    public Map<Long, Long> countProductsByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        String ids = categoryIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        List<CategoryProductCountDO> counts = mapper.countProductsByCategoryIds(ids);
        Map<Long, Long> result = new HashMap<>(counts.size());
        for (CategoryProductCountDO row : counts) {
            if (row.getCategoryId() != null && row.getProductCount() != null) {
                result.put(row.getCategoryId(), row.getProductCount().longValue());
            }
        }
        return result;
    }
}