package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.message.entity.MessageSubscription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageSubscriptionMapper extends BaseMapper<MessageSubscription> {
}
