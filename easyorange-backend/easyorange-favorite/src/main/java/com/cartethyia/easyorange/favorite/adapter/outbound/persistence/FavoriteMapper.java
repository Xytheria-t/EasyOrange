package com.cartethyia.easyorange.favorite.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<FavoriteDO> {

    List<Long> selectProductIdsByUserId(@Param("userId") String userId,
                                        @Param("offset") Long offset,
                                        @Param("limit") Long limit);

    long countByUserId(@Param("userId") String userId);

    int deleteByIds(@Param("ids") List<String> ids);

    @Select("SELECT * FROM eo_favorite WHERE user_id = #{userId} AND product_id = #{productId} AND del_flag = 2 LIMIT 1")
    FavoriteDO selectSoftDeletedByUserIdAndProductId(@Param("userId") String userId, @Param("productId") String productId);

    @Update("UPDATE eo_favorite SET del_flag = 0, update_time = NOW(), update_by = #{updateBy}, version = version + 1 WHERE id = #{id} AND del_flag = 2")
    int reviveById(@Param("id") String id, @Param("updateBy") String updateBy);
}
