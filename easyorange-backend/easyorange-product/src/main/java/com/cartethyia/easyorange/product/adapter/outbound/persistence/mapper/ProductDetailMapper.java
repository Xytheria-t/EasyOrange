package com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ProductDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductDetailMapper extends BaseMapper<ProductDetailDO> {

    @Select("<script>" +
            "SELECT * FROM eo_product_detail WHERE product_id IN " +
            "<foreach collection='productIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<ProductDetailDO> selectDetailsByProductIds(@Param("productIds") List<String> productIds);
}
