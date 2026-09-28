package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryCreateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.CategoryUpdateRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.CategoryResponse;
import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort;
import com.cartethyia.easyorange.admin.domain.port.CategoryWritePort;
import com.cartethyia.easyorange.admin.domain.port.CategoryWritePort.CategoryWriteResult;
import com.cartethyia.easyorange.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台分类管理 — **薄适配层**，不含任何分类业务规则。
 * <p>
 * 分类是资产域的概念（商品挂在分类上），它的层级 / 环 / 重名 / 删除约束全部在
 * product 模块的 {@code Category} 聚合与 {@code CategoryCommandHandler} 里。
 * 本类只做三件事：DTO → 端口入参、端口结果 → DTO、事务边界。
 * <p>
 * 早先这里是规则的实际落点（裸中文串 {@code BusinessException}、{@code MAX_CATEGORY_LEVEL} 常量、
 * 递归建树），导致「分类」在代码里读起来像后台的配置功能而不是领域对象。
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final AdminCategoryPort adminCategoryPort;
    private final CategoryWritePort categoryWritePort;

    // ==================== 查询 ====================

    /** 分类列表（含禁用，后台要能看到并恢复禁用项）。 */
    public List<CategoryResponse> listCategories(String parentId) {
        return adminCategoryPort.listCategories(parentId, true).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 整棵分类树（仅启用中）。 */
    public List<CategoryResponse> categoryTree() {
        return adminCategoryPort.categoryTree().stream().map(this::toResponse).toList();
    }

    // ==================== 写操作 ====================

    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        CategoryWriteResult result = categoryWritePort.createCategory(
                request.name(), request.parentId(), request.icon(), request.sortOrder());
        return new CategoryResponse(
                result.categoryId(),
                result.name(),
                result.parentId(),
                null,
                result.level(),
                result.sortOrder(),
                result.status(),
                result.productCount(),
                result.createTime(),
                List.of());
    }

    /**
     * 更新分类 — 挂载点变化先走移动路径，再走属性更新路径。
     * <p>
     * 两条路径分开是刻意的：移动要写 {@code parent_id = NULL} 并平移整棵子树的 level，
     * 与「改个名字」是完全不同的操作，混在一起正是旧实现静默丢失 parent_id 更新的根因。
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(String id, CategoryUpdateRequest request) {
        CategoryView existing = adminCategoryPort
                .getCategory(id)
                .orElseThrow(() -> BusinessException.of("分类不存在: id=" + id));
        // 一级分类的 parentId 就是 null，所以「是否找到」不能靠 map(parentId) 判断 ——
        // Optional.map 遇 null 会退化成 empty，根分类会被误判成不存在。
        String currentParentId = existing.parentId();
        boolean parentChanged =
                request.parentId() != null ? !request.parentId().equals(currentParentId) : currentParentId != null;

        if (parentChanged) {
            categoryWritePort.moveCategory(id, request.parentId());
        }
        CategoryWriteResult result =
                categoryWritePort.updateCategory(id, request.name(), request.sortOrder(), request.icon(), request.status());
        return new CategoryResponse(
                result.categoryId(),
                result.name(),
                result.parentId(),
                null,
                result.level(),
                result.sortOrder(),
                result.status(),
                result.productCount(),
                result.createTime(),
                List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status) {
        categoryWritePort.updateCategoryStatus(id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        categoryWritePort.deleteCategory(id);
    }

    // ==================== 私有 ====================

    private CategoryResponse toResponse(CategoryView view) {
        return new CategoryResponse(
                view.id(),
                view.name(),
                view.parentId(),
                view.parentName(),
                view.level(),
                view.sortOrder(),
                view.status(),
                view.productCount(),
                view.createTime(),
                view.children() == null
                        ? List.of()
                        : view.children().stream().map(this::toResponse).toList());
    }
}
