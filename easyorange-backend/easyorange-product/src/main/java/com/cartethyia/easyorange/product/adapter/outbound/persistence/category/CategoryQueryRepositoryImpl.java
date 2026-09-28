package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryQueryRepositoryImpl extends BaseRepository<CategoryMapper, CategoryDO>
        implements CategoryQueryRepository {

    public CategoryQueryRepositoryImpl(CategoryMapper categoryMapper) {
        super(categoryMapper);
    }

    @Override
    public List<CategoryReadModel> findEnabledByParentId(String parentId) {
        return lambdaQuery()
                .eq(CategoryDO::getParentId, parentId)
                .eq(CategoryDO::getStatus, CategoryStatus.ENABLED)
                .orderByAsc(CategoryDO::getSortOrder)
                .list()
                .stream()
                .map(this::toReadModel)
                .toList();
    }

    @Override
    public List<CategoryReadModel> findEnabledByLevel(Integer level) {
        return lambdaQuery()
                .eq(CategoryDO::getLevel, level)
                .eq(CategoryDO::getStatus, CategoryStatus.ENABLED)
                .orderByAsc(CategoryDO::getSortOrder)
                .list()
                .stream()
                .map(this::toReadModel)
                .toList();
    }

    @Override
    public List<CategoryReadModel> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(ids).stream().map(this::toReadModel).toList();
    }

    @Override
    public Map<String, Long> countAllProductsByCategoryIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return toCountMap(mapper.countAllProductsByCategoryIds(categoryIds));
    }

    @Override
    public Map<String, Long> countOnlineProductsByCategoryIdsWithChildren(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return toCountMap(mapper.countOnlineProductsByCategoryIdsWithChildren(categoryIds));
    }

    @Override
    public boolean existsById(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return false;
        }
        return mapper.selectCount(Wrappers.<CategoryDO>lambdaQuery().eq(CategoryDO::getId, categoryId)) > 0;
    }

    private Map<String, Long> toCountMap(List<CategoryProductCount> counts) {
        Map<String, Long> result = new HashMap<>(counts.size());
        for (CategoryProductCount row : counts) {
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
                0);
    }
}
