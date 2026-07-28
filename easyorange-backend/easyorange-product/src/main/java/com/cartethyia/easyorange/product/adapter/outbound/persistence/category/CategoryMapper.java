package com.cartethyia.easyorange.product.adapter.outbound.persistence.category;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryProductCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    @Select("<script>"
            + "SELECT category_id, COUNT(*) AS product_count "
            + "FROM eo_product "
            + "WHERE del_flag = 0 AND status = 1 "
            + "AND category_id IN "
            + "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY category_id"
            + "</script>")
    List<CategoryProductCount> countProductsByCategoryIds(@Param("categoryIds") List<String> categoryIds);

    /**
     * 统计指定分类（含子分类）下的在售商品数量。
     * 通过 LEFT JOIN eo_category 将子分类的商品计数归到父分类：
     * - 商品在子分类 (category_id=10, parent_id=1) → COALESCE 返回 parent_id=1
     * - 商品直接在父分类 (category_id=1, parent_id=null) → COALESCE 返回 category_id=1
     */
    @Select("<script>"
            + "SELECT COALESCE(c.parent_id, p.category_id) AS category_id, COUNT(*) AS product_count "
            + "FROM eo_product p "
            + "LEFT JOIN eo_category c ON p.category_id = c.id AND c.del_flag = 0 "
            + "WHERE p.del_flag = 0 AND p.status = 1 "
            + "AND (p.category_id IN "
            + "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>#{id}</foreach> "
            + "OR c.parent_id IN "
            + "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>#{id}</foreach>) "
            + "GROUP BY COALESCE(c.parent_id, p.category_id)"
            + "</script>")
    List<CategoryProductCount> countProductsByCategoryIdsWithChildren(@Param("categoryIds") List<String> categoryIds);
}
