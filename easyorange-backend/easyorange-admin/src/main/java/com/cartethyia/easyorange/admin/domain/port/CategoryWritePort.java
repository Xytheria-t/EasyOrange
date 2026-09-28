package com.cartethyia.easyorange.admin.domain.port;

/**
 * 分类写侧端口 — 分类是资产域概念，写操作经此端口交给 product 模块的
 * {@code CategoryCommandHandler} 执行，规则（层级 / 环 / 重名 / 删除前置条件）不在 admin 侧实现。
 * <p>
 * 只含 JDK 类型：admin 模块不依赖 product，翻译在 application 模块的适配器里做。
 */
public interface CategoryWritePort {

    /** 新建分类；{@code parentId} 为 null 表示一级分类。 */
    CategoryWriteResult createCategory(String name, String parentId, String icon, Integer sortOrder);

    /** 更新属性（名称 / 排序 / 图标 / 状态），**不移动挂载点**。 */
    CategoryWriteResult updateCategory(String id, String name, Integer sortOrder, String icon, Integer status);

    /** 移动挂载点；{@code newParentId} 为 null 表示移到一级。 */
    CategoryWriteResult moveCategory(String id, String newParentId);

    /** 启用/禁用。 */
    void updateCategoryStatus(String id, Integer status);

    /** 删除（有子分类或关联商品时由领域层拒绝）。 */
    void deleteCategory(String id);

    /**
     * 写操作结果 — 只回传展示所需字段，不把整个分类聚合漏过模块边界。
     *
     * @param categoryId 分类 id
     * @param name 分类名
     * @param parentId 父分类 id，一级为 null
     * @param level 层级
     * @param sortOrder 排序值
     * @param status 1 启用 / 0 禁用
     * @param productCount 关联商品数
     * @param createTime 创建时间
     */
    record CategoryWriteResult(
            String categoryId,
            String name,
            String parentId,
            Integer level,
            Integer sortOrder,
            Integer status,
            Long productCount,
            java.time.LocalDateTime createTime) {}
}
