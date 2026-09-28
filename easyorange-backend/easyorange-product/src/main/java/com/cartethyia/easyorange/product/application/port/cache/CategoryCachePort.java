package com.cartethyia.easyorange.product.application.port.cache;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;

public interface CategoryCachePort {

    /**
     * 取某父分类下的启用中子节点。
     *
     * @param parentId 父分类 id；null 表示一级分类
     */
    List<CategoryReadModel> getCategoriesByParentId(String parentId);

    /**
     * 整份失效 — 任何分类写操作后调用。
     * <p>
     * 旧设计按 {@code level:N} / {@code parent:X} 逐 key 失效，写路径要枚举所有受影响的 key，
     * 漏一个就是脏数据（且漏了不报错、只是分类列表不对）。类目是低频写，整份清空的代价可以忽略，
     * 换来的是「失效不会漏」这个可推理的性质。
     */
    void evictAll();
}
