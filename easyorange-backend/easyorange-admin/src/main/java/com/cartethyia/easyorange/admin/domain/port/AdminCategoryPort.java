package com.cartethyia.easyorange.admin.domain.port;

import com.cartethyia.easyorange.admin.domain.model.CategoryView;
import java.util.List;

/**
 * Admin 侧分类端口 — 防腐层：admin 只看见 {@link CategoryView}，看不见分类聚合、DO 或 Mapper。
 * <p>
 * 写侧的层级 / 环 / 重名 / 删除校验**不在这里实现**（那是 product 模块 {@code Category} 聚合与
 * {@code CategoryCommandHandler} 的职责）。本端口存在的意义只是让 admin 的 Controller 有个
 * 不依赖 product 内部结构的入口，实现类在 application 模块做一次薄转发。
 */
public interface AdminCategoryPort {

    /**
     * 查单个分类。
     *
     * @return 不存在时为空
     */
    java.util.Optional<CategoryView> getCategory(String categoryId);

    /**
     * 分类列表。
     *
     * @param parentId 父分类 id；null 表示查一级分类
     * @param includeDisabled 是否包含禁用分类（后台管理要看得见，C 端不要）
     */
    List<CategoryView> listCategories(String parentId, boolean includeDisabled);

    /** 整棵分类树（仅启用中，后台建树用）。 */
    List<CategoryView> categoryTree();
}
