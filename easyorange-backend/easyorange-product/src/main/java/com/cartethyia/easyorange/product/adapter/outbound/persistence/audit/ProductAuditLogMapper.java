package com.cartethyia.easyorange.product.adapter.outbound.persistence.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductAuditLogMapper extends BaseMapper<ProductAuditLogDO> {

    List<ProductAuditLogDO> selectByProductId(@Param("productId") String productId);
}
