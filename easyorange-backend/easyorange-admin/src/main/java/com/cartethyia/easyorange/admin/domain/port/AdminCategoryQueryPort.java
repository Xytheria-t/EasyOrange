package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的分类查询/操作端口
 * 用于跨模块查询与操作分类信息，遵循防腐层原则
 */
public interface AdminCategoryQueryPort {

    /**
     * 查询分类，不存在或已删除时返回 null
     */
    CategoryRecord getCategory(String categoryId);

    /**
     * 查询分类列表（parentId 为 null 时返回全部分类，否则返回子分类），按 sortOrder 升序
     */
    List<CategoryRecord> listCategories(String parentId);

    /**
     * 按 ID 列表批量查询分类
     */
    List<CategoryRecord> getCategoriesByIds(List<String> ids);

    /**
     * 按名称查询分类（第一个匹配），不存在时返回 null
     */
    CategoryRecord findCategoryByName(String name);

    /**
     * 创建分类并返回带 ID 的分类记录
     */
    CategoryRecord createCategory(String name, String parentId, Integer sortOrder, Integer level);

    /**
     * 更新分类（全字段）
     */
    void updateCategory(CategoryRecord category);

    /**
     * 删除分类（逻辑删除），子分类/关联商品校验由调用方负责
     */
    void deleteCategory(String categoryId);

    /**
     * 统计分类的直接子分类数量
     */
    long countCategoryChildren(String categoryId);

    /**
     * 按分类 ID 列表统计关联商品数
     */
    Map<String, Long> countProductsByCategoryIds(List<String> categoryIds);

    /**
     * 分类记录
     */
    record CategoryRecord(
            String id,
            String name,
            String parentId,
            Integer level,
            String icon,
            Integer sortOrder,
            Integer status,
            LocalDateTime createTime) {}
}
