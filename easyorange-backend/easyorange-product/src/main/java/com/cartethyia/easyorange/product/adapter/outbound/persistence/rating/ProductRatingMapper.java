package com.cartethyia.easyorange.product.adapter.outbound.persistence.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductRatingMapper extends BaseMapper<ProductRatingDO> {

    @Select("SELECT rating, COUNT(*) as count FROM eo_product_review "
            + "WHERE product_id = #{productId} AND del_flag = 0 AND status = 1 "
            + "GROUP BY rating")
    List<Map<String, Object>> countByRating(@Param("productId") String productId);

    @Update("UPDATE eo_product_review SET likes = likes + 1 WHERE id = #{reviewId} AND del_flag = 0")
    int incrementLikes(@Param("reviewId") String reviewId);
}
