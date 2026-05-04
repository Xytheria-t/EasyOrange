package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.product.domain.repository.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryProductCountDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CategoryQueryRepositoryImpl implements CategoryQueryRepository {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryDO> findByParentId(Long parentId) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getParentId, parentId)
                        .orderByAsc(CategoryDO::getSortOrder)
        );
    }

    @Override
    public List<CategoryDO> findByLevel(Integer level) {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getLevel, level)
                        .orderByAsc(CategoryDO::getSortOrder)
        );
    }

    @Override
    public CategoryDO findByName(String name) {
        return categoryMapper.selectOne(
                new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getName, name)
        );
    }

    @Override
    public List<CategoryDO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryMapper.selectBatchIds(ids);
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
        
        List<CategoryProductCountDO> counts = categoryMapper.countProductsByCategoryIds(ids);
        Map<Long, Long> result = new HashMap<>(counts.size());
        for (CategoryProductCountDO row : counts) {
            if (row.getCategoryId() != null && row.getProductCount() != null) {
                result.put(row.getCategoryId(), row.getProductCount().longValue());
            }
        }
        return result;
    }
}
