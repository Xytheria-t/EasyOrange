package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryQueryPort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 分类查询/操作适配器
 * <p>
 * 实现 {@link AdminCategoryQueryPort}，通过 Category Mapper / Repository 访问分类数据并转换为 Admin 模块需要的格式。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminCategoryQueryAdapter implements AdminCategoryQueryPort {

    private final CategoryMapper categoryMapper;
    private final CategoryQueryRepository categoryQueryRepository;

    @Override
    public CategoryRecord getCategory(String categoryId) {
        CategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDelFlag() != 0) {
            return null;
        }
        return toCategoryRecord(category);
    }

    @Override
    public List<CategoryRecord> listCategories(String parentId) {
        if (parentId == null) {
            return ChainWrappers.lambdaQueryChain(categoryMapper)
                    .eq(CategoryDO::getDelFlag, 0)
                    .orderByAsc(CategoryDO::getSortOrder)
                    .list()
                    .stream()
                    .map(this::toCategoryRecord)
                    .toList();
        }
        return categoryQueryRepository.findByParentId(parentId).stream()
                .map(this::toCategoryRecord)
                .toList();
    }

    @Override
    public List<CategoryRecord> getCategoriesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryQueryRepository.findByIds(ids).stream()
                .map(this::toCategoryRecord)
                .toList();
    }

    @Override
    public CategoryRecord findCategoryByName(String name) {
        CategoryReadModel existing = categoryQueryRepository.findByName(name);
        return existing != null ? toCategoryRecord(existing) : null;
    }

    @Override
    public CategoryRecord createCategory(String name, String parentId, Integer sortOrder, Integer level) {
        CategoryDO entity = CategoryDO.builder()
                .name(name)
                .parentId(parentId)
                .level(level)
                .sortOrder(sortOrder)
                .status(1)
                .build();

        categoryMapper.insert(entity);
        return toCategoryRecord(entity);
    }

    @Override
    public void updateCategory(CategoryRecord category) {
        CategoryDO entity = categoryMapper.selectById(category.id());
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }
        entity.setName(category.name());
        entity.setParentId(category.parentId());
        entity.setLevel(category.level());
        entity.setSortOrder(category.sortOrder());
        entity.setStatus(category.status());
        categoryMapper.updateById(entity);
    }

    @Override
    public void deleteCategory(String categoryId) {
        CategoryDO entity = categoryMapper.selectById(categoryId);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }
        entity.setDelFlag(2);
        categoryMapper.updateById(entity);
    }

    @Override
    public long countCategoryChildren(String categoryId) {
        return ChainWrappers.lambdaQueryChain(categoryMapper)
                .eq(CategoryDO::getParentId, categoryId)
                .eq(CategoryDO::getDelFlag, 0)
                .count();
    }

    @Override
    public Map<String, Long> countProductsByCategoryIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryQueryRepository.countProductsByCategoryIds(categoryIds);
    }

    private CategoryRecord toCategoryRecord(CategoryDO category) {
        return new CategoryRecord(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getIcon(),
                category.getSortOrder(),
                category.getStatus(),
                category.getCreateTime());
    }

    private CategoryRecord toCategoryRecord(CategoryReadModel model) {
        return new CategoryRecord(
                model.id(),
                model.name(),
                model.parentId(),
                model.level(),
                model.icon(),
                model.sortOrder(),
                model.status(),
                model.createTime());
    }
}
