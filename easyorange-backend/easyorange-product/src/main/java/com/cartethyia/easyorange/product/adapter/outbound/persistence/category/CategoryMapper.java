package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    /** 子树递归展开的深度上限 —— 领域层只允许 3 级，这里放宽到 20 仅为脏数据兜底。 */
    int MAX_SUBTREE_DEPTH = 20;

    /**
     * 统计指定分类下**直接挂载**的在售商品数。
     * <p>
     * 不含子分类。删除前的「是否有关联商品」判定必须用它 —— 用「在售」口径会让
     * 只挂下架/已售商品的分类被误判为空从而删掉，留下悬空 category_id。
     */
    @Select("<script>"
            + "SELECT category_id, COUNT(*) AS product_count "
            + "FROM eo_product "
            + "WHERE del_flag = 0 AND category_id IN "
            + "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY category_id"
            + "</script>")
    List<CategoryProductCount> countAllProductsByCategoryIds(@Param("categoryIds") List<String> categoryIds);

    /**
     * 统计指定分类下**在售**商品数（含子分类聚合，任意深度）。
     * <p>
     * 用递归 CTE 而非单层 JOIN：类目树允许 3 级，单层 {@code LEFT JOIN eo_category ON parent_id} 只能把
     * 商品归到直接父级，最深一层（3 级）的商品在一级分类行上会漏计。
     * <p>
     * 形状：{@code subtree} 展开「每个 root → 它自己及全部后代」的 (root, category) 对，
     * 再把每个分类下的在售商品挂到对应的 root 上，最后按 root 汇总。
     * 商品只属于一个分类，理论上不会重复计入；仍用 {@code COUNT(DISTINCT p.id)} 兜底，
     * 避免将来引入多分类挂载时口径静默翻倍。
     */
    @Select("<script>"
            + "WITH RECURSIVE subtree AS ("
            + "  SELECT c.id AS category_id, c.id AS root_id FROM eo_category c"
            + "  WHERE c.del_flag = 0 AND c.id IN "
            + "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "  UNION ALL"
            + "  SELECT c.id, s.root_id FROM eo_category c"
            + "  JOIN subtree s ON c.parent_id = s.category_id WHERE c.del_flag = 0"
            + ") "
            + "SELECT s.root_id AS category_id, COUNT(DISTINCT p.id) AS product_count "
            + "FROM subtree s JOIN eo_product p ON p.category_id = s.category_id "
            + "WHERE p.del_flag = 0 AND p.status = 'ONLINE' "
            + "GROUP BY s.root_id"
            + "</script>")
    List<CategoryProductCount> countOnlineProductsByCategoryIdsWithChildren(@Param("categoryIds") List<String> categoryIds);

    /**
     * 查某分类子树内的全部 id（**含自身**）—— 成环检测与整棵子树 level 平移的输入。
     * <p>
     * 用递归 CTE 而非在 Java 里逐层下钻：一次查询拿全树，且不受「层数」假设限制。
     * 带 depth 计数并在超过 {@link #MAX_SUBTREE_DEPTH} 处截断，脏数据成环时递归不会无限膨胀把库拖死。
     */
    @Select("WITH RECURSIVE subtree AS ("
            + "  SELECT id, 0 AS depth FROM eo_category WHERE id = #{rootId} AND del_flag = 0"
            + "  UNION ALL"
            + "  SELECT c.id, s.depth + 1 FROM eo_category c"
            + "  JOIN subtree s ON c.parent_id = s.id"
            + "  WHERE c.del_flag = 0 AND s.depth < " + MAX_SUBTREE_DEPTH
            + ") SELECT id FROM subtree")
    List<String> selectSubtreeIds(@Param("rootId") String rootId);
}
