package com.cartethyia.easyorange.message.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.entity.MessageTemplate;
import com.cartethyia.easyorange.message.mapper.MessageTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MybatisMessageTemplateRepository implements MessageTemplateRepository {

    private final MessageTemplateMapper mapper;

    @Override
    public MessageTemplate findByCode(String templateCode) {
        return mapper.selectOne(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .eq(MessageTemplate::getStatus, MessageConstant.TEMPLATE_STATUS_ENABLED));
    }

    @Override
    public MessageTemplate save(MessageTemplate template) {
        mapper.insert(template);
        return template;
    }

    @Override
    public void update(MessageTemplate template) {
        mapper.updateById(template);
    }

    @Override
    public void deleteByIds(Long[] templateIds) {
        mapper.deleteBatchIds(Arrays.asList(templateIds));
    }

    @Override
    public List<MessageTemplate> findByCondition(MessageTemplate condition) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        if (condition != null) {
            if (condition.getTemplateCode() != null) {
                wrapper.like(MessageTemplate::getTemplateCode, condition.getTemplateCode());
            }
            if (condition.getTemplateName() != null) {
                wrapper.like(MessageTemplate::getTemplateName, condition.getTemplateName());
            }
            if (condition.getTemplateType() != null) {
                wrapper.eq(MessageTemplate::getTemplateType, condition.getTemplateType());
            }
            if (condition.getStatus() != null) {
                wrapper.eq(MessageTemplate::getStatus, condition.getStatus());
            }
        }
        wrapper.orderByDesc(MessageTemplate::getCreateTime);
        return mapper.selectList(wrapper);
    }

    @Override
    public boolean existsByCodeExcludingId(String templateCode, Long excludeId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .ne(excludeId != null, MessageTemplate::getId, excludeId));
        return count > 0;
    }
}
