package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplateDO> {
}
