package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.CategoryWritePort;
import com.cartethyia.easyorange.product.application.command.CategoryCommandHandler;
import com.cartethyia.easyorange.product.domain.aggregate.Category;
import com.cartethyia.easyorange.product.domain.enums.CategoryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 分类写侧适配器 — 实现 admin 的 {@link CategoryWritePort}，把调用转给 product 模块的
 * {@link CategoryCommandHandler}。
 * <p>
 * 这层只做**翻译**（聚合 → 只含 JDK 类型的 {@link CategoryWriteResult}），不含任何规则：
 * 层级、环、重名、删除前置条件都由 product 侧判定。这样 admin 模块无需依赖 product，
 * 分类的业务约束也只有一个事实来源。
 */
@Primary
@Component
@RequiredArgsConstructor
public class CategoryWriteAdapter implements CategoryWritePort {

    private final CategoryCommandHandler categoryCommandHandler;

    @Override
    public CategoryWriteResult createCategory(String name, String parentId, String icon, Integer sortOrder) {
        return toWriteResult(categoryCommandHandler.createCategory(name, parentId, icon, sortOrder));
    }

    @Override
    public CategoryWriteResult updateCategory(String id, String name, Integer sortOrder, String icon, Integer status) {
        return toWriteResult(categoryCommandHandler.updateCategory(id, name, sortOrder, icon, status));
    }

    @Override
    public CategoryWriteResult moveCategory(String id, String newParentId) {
        return toWriteResult(categoryCommandHandler.moveCategory(id, newParentId));
    }

    @Override
    public void updateCategoryStatus(String id, Integer status) {
        categoryCommandHandler.updateCategoryStatus(id, CategoryStatus.fromCode(String.valueOf(status)));
    }

    @Override
    public void deleteCategory(String id) {
        categoryCommandHandler.deleteCategory(id);
    }

    /** 新建的分类还没有商品，计数恒为 0。 */
    private CategoryWriteResult toWriteResult(Category category) {
        return new CategoryWriteResult(
                category.getId().value(),
                category.getName().value(),
                category.getParentId() != null ? category.getParentId().value() : null,
                category.getLevel(),
                category.getSortOrder(),
                category.getStatus() != null && category.getStatus().isEnabled() ? 1 : 0,
                0L,
                category.getCreateTime());
    }
}
