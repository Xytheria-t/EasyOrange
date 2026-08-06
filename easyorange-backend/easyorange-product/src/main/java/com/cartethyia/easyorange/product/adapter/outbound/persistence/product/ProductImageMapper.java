package com.cartethyia.easyorange.product.adapter.outbound.persistence.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImageDO> {

    void batchInsert(@Param("images") List<ProductImageDO> images);

    void deleteByProductIdAndUrls(@Param("productId") String productId, @Param("urls") List<String> urls);
}
