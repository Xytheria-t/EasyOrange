package com.cartethyia.easyorange.product.application.port.query;

import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.util.List;
import java.util.Map;

public interface CategoryQueryRepository {

    /**
     * 查某父分类下的**启用中**直接子节点。
     * <p>
     * 禁用分类一律不出现在读路径 —— 这是「禁用」的唯一语义落点。
     *
     * @param parentId 父分类 id；null / 空表示查一级分类
     */
    List<CategoryReadModel> findEnabledByParentId(String parentId);

    /** 按 id 批量查（不过滤 status：删除前的存在性校验需要看到禁用节点）。 */
    List<CategoryReadModel> findByIds(List<String> ids);

    /**
     * 统计各分类下**直接挂载**的商品数（不限上下架状态）。
     * <p>
     * 删除前的存在性校验必须用这个口径：若按「在售」统计，只挂下架/已售商品的分类
     * 会被判成空而被删掉，留下悬空 category_id。
     */
    Map<String, Long> countAllProductsByCategoryIds(List<String> categoryIds);

    /** 统计各分类下在售商品数（含子分类，任意深度聚合）。 */
    Map<String, Long> countOnlineProductsByCategoryIdsWithChildren(List<String> categoryIds);
}
