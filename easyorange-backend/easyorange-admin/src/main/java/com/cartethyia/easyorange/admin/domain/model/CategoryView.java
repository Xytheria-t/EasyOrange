package com.cartethyia.easyorange.admin.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * admin 侧的分类只读视图 — 跨模块传递用，字段名与 product 模块的 {@code CategoryReadModel} 对齐
 * （统一用 {@code id}，不再有 admin/product 两套 {@code id} / {@code categoryId} 命名）。
 * <p>
 * 刻意只含 JDK 类型：admin 模块不依赖 product（见 ADR 0006 的端口隔离），
 * 领域枚举在 {@code AdminCategoryAdapter} 里翻译成本视图的 {@code Integer}。
 *
 * @param status 1 启用 / 0 禁用
 * @param productCount 关联商品数：一级分类为整棵子树聚合，其余为直接挂载
 */
public record CategoryView(
        String id,
        String name,
        /** 一级分类为 null。 */
        String parentId,
        String parentName,
        Integer level,
        Integer sortOrder,
        Integer status,
        Long productCount,
        LocalDateTime createTime,
        List<CategoryView> children) {}
