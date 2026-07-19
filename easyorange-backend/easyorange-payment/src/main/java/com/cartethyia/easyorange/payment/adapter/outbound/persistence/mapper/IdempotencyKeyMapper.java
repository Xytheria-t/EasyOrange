package com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.IdempotencyKeyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyKeyMapper extends BaseMapper<IdempotencyKeyDO> {
}
