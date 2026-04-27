package com.cartethyia.easyorange.product.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.entity.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryAggregate {

    private final Category category;
    private final List<Category> children;

    public static CategoryAggregate create(Long parentId, String name, Integer level, Integer sortOrder) {
        if (name == null || name.isBlank()) {
            throw BusinessException.of("分类名称不能为空");
        }
        if (level == null || level < 1 || level > 3) {
            throw BusinessException.of("分类层级必须在 1-3 之间");
        }

        Category category = Category.builder()
                .parentId(parentId)
                .name(name)
                .level(level)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .status(1)
                .build();

        return new CategoryAggregate(category, new ArrayList<>());
    }

    public static CategoryAggregate load(Category category, List<Category> children) {
        if (category == null) {
            return null;
        }
        return new CategoryAggregate(category, children != null ? children : new ArrayList<>());
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw BusinessException.of("分类名称不能为空");
        }
        category.setName(name);
    }

    public void updateSortOrder(Integer sortOrder) {
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
    }

    public void updateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.of("状态必须为 0 或 1");
        }
        category.setStatus(status);
    }

    public void disable() {
        category.setStatus(0);
    }

    public void enable() {
        category.setStatus(1);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public Long getId() {
        return category.getId();
    }

    public Long getParentId() {
        return category.getParentId();
    }

    public String getName() {
        return category.getName();
    }

    public Integer getLevel() {
        return category.getLevel();
    }

    public Integer getSortOrder() {
        return category.getSortOrder();
    }

    public Integer getStatus() {
        return category.getStatus();
    }

    public Category toEntity() {
        return category;
    }
}
