package com.cartethyia.easyorange.admin.application.service;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.model.CategoryUpdateCommand;
import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort;
import com.cartethyia.easyorange.admin.domain.port.AdminCategoryWritePort.CategoryWriteResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台分类管理 — **薄适配层**，不含任何分类业务规则。
 * <p>
 * <b>取舍</b>：分类是资产域的概念（商品挂在分类上），它的层级 / 环 / 重名 / 删除约束全部在
 * product 模块的 {@code Category} 聚合与 {@code CategoryCommandHandler} 里。
 * 本类只做三件事：把 web 的入参收成 {@link CategoryUpdateCommand}、决定走移动还是属性更新、划事务边界；
 * 出入参一律是 {@code domain} 的视图与写侧结果，HTTP 形状归 web 侧的 assembler。
 * <p>
 * <b>边界</b>：分类不存在时 {@link AdminDomainException#categoryNotFound}；
 * 规则不合法时端口抛什么就上抛什么，本类不换一种异常类型。
 */
@Service
@RequiredArgsConstructor
public class AdminCategoryAppService {

    private final AdminCategoryPort adminCategoryPort;
    private final AdminCategoryWritePort categoryWritePort;

    public List<CategoryView> listCategories(String parentId) {
        // includeDisabled 恒为 true：后台要能看见并恢复被禁用的分类
        return adminCategoryPort.listCategories(parentId, true);
    }

    public List<CategoryView> categoryTree() {
        return adminCategoryPort.categoryTree();
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryWriteResult createCategory(String name, String parentId, String icon, Integer sortOrder) {
        return categoryWritePort.createCategory(name, parentId, icon, sortOrder);
    }

    /**
     * 更新分类 — 挂载点变化先走移动路径，再走属性更新路径。
     * <p>
     * 两条路径分开是刻意的：移动要写 {@code parent_id = NULL} 并平移整棵子树的 level，
     * 与「改个名字」是完全不同的操作，混在一起正是旧实现静默丢失 parent_id 更新的根因。
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryWriteResult updateCategory(String id, CategoryUpdateCommand command) {
        CategoryView existing =
                adminCategoryPort.getCategory(id).orElseThrow(() -> AdminDomainException.categoryNotFound(id));
        // 一级分类的 parentId 就是 null，所以「是否找到」不能靠 map(parentId) 判断 ——
        // Optional.map 遇 null 会退化成 empty，根分类会被误判成不存在。
        String currentParentId = existing.parentId();
        boolean parentChanged =
                command.parentId() != null ? !command.parentId().equals(currentParentId) : currentParentId != null;

        if (parentChanged) {
            categoryWritePort.moveCategory(id, command.parentId());
        }
        return categoryWritePort.updateCategory(
                id, command.name(), command.sortOrder(), command.icon(), command.status());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String id, Integer status) {
        categoryWritePort.updateCategoryStatus(id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        categoryWritePort.deleteCategory(id);
    }
}
