package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryProductCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<CategoryDO> {

    @Select("SELECT category_id, COUNT(*) AS product_count " +
            "FROM eo_product " +
            "WHERE del_flag = 0 AND status = 1 " +
            "AND category_id IN (${ids}) " +
            "GROUP BY category_id")
    List<CategoryProductCountDO> countProductsByCategoryIds(@Param("ids") String ids);
}
