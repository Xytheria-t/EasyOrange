package com.cartethyia.easyorange.favorite.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<FavoriteDO> {

    List<Long> selectProductIdsByUserId(@Param("userId") Long userId,
                                        @Param("offset") Long offset,
                                        @Param("limit") Long limit);

    long countByUserId(@Param("userId") Long userId);

    int deleteByIds(@Param("ids") List<Long> ids);
}
