package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CategoryQueryRepositoryImpl extends BaseRepository<CategoryMapper, CategoryDO>
        implements CategoryQueryRepository {

    public CategoryQueryRepositoryImpl(CategoryMapper categoryMapper) {
        super(categoryMapper);
    }

    @Override
    public List<CategoryReadModel> findEnabledByParentId(String parentId) {
        // parentId 为空表示查一级分类：必须走 isNull()，eq(col, null) 生成的是
        // `parent_id = NULL` —— SQL 里恒不成立，一级分类会一条都查不出来。
        return lambdaQuery()
                .eq(parentId != null && !parentId.isBlank(), CategoryDO::getParentId, parentId)
                .isNull(parentId == null || parentId.isBlank(), CategoryDO::getParentId)
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
