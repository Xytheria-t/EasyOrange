package com.cartethyia.easyorange.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.infrastructure.persistence.dataobject.ProductDetailDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductDetailMapper extends BaseMapper<ProductDetailDO> {

    @Select("<script>" +
            "SELECT * FROM product_detail WHERE product_id IN " +
            "<foreach collection='productIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<ProductDetailDO> selectDetailsByProductIds(@Param("productIds") List<Long> productIds);
}