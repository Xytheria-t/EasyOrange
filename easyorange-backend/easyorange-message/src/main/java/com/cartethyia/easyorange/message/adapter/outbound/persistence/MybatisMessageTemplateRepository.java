package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageTemplateDO;
import com.cartethyia.easyorange.message.domain.repository.MessageTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class MybatisMessageTemplateRepository extends BaseRepository<MessageTemplateMapper, MessageTemplateDO> implements MessageTemplateRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MybatisMessageTemplateRepository(MessageTemplateMapper mapper, MessageDataMapper messageDataMapper) {
        super(mapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public MessageTemplateAggregate findByCode(String templateCode) {
        MessageTemplateDO entity = lambdaQuery()
                .eq(MessageTemplateDO::getTemplateCode, templateCode)
                .eq(MessageTemplateDO::getStatus, MessageConstant.TEMPLATE_STATUS_ENABLED)
                .one();
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public MessageTemplateAggregate save(MessageTemplateAggregate template) {
        MessageTemplateDO entity = messageDataMapper.toEntity(template);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public void update(MessageTemplateAggregate template) {
        mapper.updateById(messageDataMapper.toEntity(template));
    }

    @Override
    public void deleteByIds(String[] templateIds) {
        mapper.deleteByIds(Arrays.asList(templateIds));
    }

    @Override
    public List<MessageTemplateAggregate> findByCondition(MessageTemplateAggregate condition) {
        var wrapper = lambdaQuery();
        if (condition != null) {
            if (condition.templateCode() != null) {
                wrapper.like(MessageTemplateDO::getTemplateCode, condition.templateCode());
            }
            if (condition.templateName() != null) {
                wrapper.like(MessageTemplateDO::getTemplateName, condition.templateName());
            }
            if (condition.templateType() != null) {
                wrapper.eq(MessageTemplateDO::getTemplateType, condition.templateType());
            }
            if (condition.status() != null) {
                wrapper.eq(MessageTemplateDO::getStatus, condition.status());
            }
        }
        wrapper.orderByDesc(MessageTemplateDO::getCreateTime);
        return messageDataMapper.toTemplateAggregateList(wrapper.list());
    }

    @Override
    public boolean existsByCodeExcludingId(String templateCode, String excludeId) {
        Long count = lambdaQuery()
                .eq(MessageTemplateDO::getTemplateCode, templateCode)
                .ne(excludeId != null, MessageTemplateDO::getId, excludeId)
                .count();
        return count > 0;
    }
}
