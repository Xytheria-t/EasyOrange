package com.cartethyia.easyorange.product.adapter.outbound.persistence.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.audit.ProductAuditLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductAuditLogMapper extends BaseMapper<ProductAuditLogDO> {

    List<ProductAuditLogDO> selectByProductId(@Param("productId") String productId);
}
