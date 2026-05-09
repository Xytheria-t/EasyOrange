package com.cartethyia.easyorange.framework.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessagePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessagePO> {
}
