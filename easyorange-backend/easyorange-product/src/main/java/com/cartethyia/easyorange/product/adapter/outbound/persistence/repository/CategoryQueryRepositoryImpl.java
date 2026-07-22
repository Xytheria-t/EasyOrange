package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.CategoryProductCountDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.CategoryMapper;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
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
    public List<CategoryReadModel> findByParentId(String parentId) {
        return lambdaQuery()
                .eq(CategoryDO::getParentId, parentId)
                .orderByAsc(CategoryDO::getSortOrder)
                .list()
                .stream()
                .map(this::toReadModel)
                .toList();
    }

    @Override
    public List<CategoryReadModel> findByLevel(Integer level) {
        return lambdaQuery()
                .eq(CategoryDO::getLevel, level)
                .orderByAsc(CategoryDO::getSortOrder)
                .list()
                .stream()
                .map(this::toReadModel)
                .toList();
    }

    @Override
    public CategoryReadModel findByName(String name) {
        CategoryDO category = lambdaQuery()
                .eq(CategoryDO::getName, name)
                .one();
        return category != null ? toReadModel(category) : null;
    }

    @Override
    public List<CategoryReadModel> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids).stream()
                .map(this::toReadModel)
                .toList();
    }

    @Override
    public Map<String, Long> countProductsByCategoryIds(List<String> categoryIds) {
        return doCount(categoryIds, false);
    }

    @Override
    public Map<String, Long> countProductsByCategoryIdsWithChildren(List<String> categoryIds) {
        return doCount(categoryIds, true);
    }

    private Map<String, Long> doCount(List<String> categoryIds, boolean withChildren) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        List<CategoryProductCountDO> counts = withChildren
                ? mapper.countProductsByCategoryIdsWithChildren(categoryIds)
                : mapper.countProductsByCategoryIds(categoryIds);
        Map<String, Long> result = new HashMap<>(counts.size());
        for (CategoryProductCountDO row : counts) {
            if (row.getCategoryId() != null && row.getProductCount() != null) {
                result.put(row.getCategoryId(), row.getProductCount().longValue());
            }
        }
        return result;
    }

    private CategoryReadModel toReadModel(CategoryDO category) {
        return new CategoryReadModel(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getIcon(),
                category.getSortOrder(),
                category.getStatus(),
                category.getCreateTime(),
                0
        );
    }
}