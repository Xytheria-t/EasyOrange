package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReviewDO> {

    @Select("SELECT rating, COUNT(*) as count FROM eo_product_review " +
            "WHERE product_id = #{productId} AND del_flag = 0 AND status = 1 " +
            "GROUP BY rating")
    List<Map<String, Object>> countByRating(@Param("productId") String productId);

    @Update("UPDATE eo_product_review SET likes = likes + 1 WHERE id = #{reviewId} AND del_flag = 0")
    int incrementLikes(@Param("reviewId") String reviewId);
}
