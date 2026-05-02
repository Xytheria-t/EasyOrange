package com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.DomainEventPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DomainEventMapper extends BaseMapper<DomainEventPO> {
}
