package com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.IdempotencyKeyPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotencyKeyMapper extends BaseMapper<IdempotencyKeyPO> {
}
