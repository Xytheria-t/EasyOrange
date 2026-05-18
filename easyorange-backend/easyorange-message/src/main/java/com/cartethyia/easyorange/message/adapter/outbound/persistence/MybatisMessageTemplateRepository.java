package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.entity.MessageTemplate;
import com.cartethyia.easyorange.message.domain.repository.MessageTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class MybatisMessageTemplateRepository extends BaseRepository<MessageTemplateMapper, MessageTemplate> implements MessageTemplateRepository {

    public MybatisMessageTemplateRepository(MessageTemplateMapper mapper) {
        super(mapper);
    }

    @Override
    public MessageTemplate findByCode(String templateCode) {
        return lambdaQuery()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .eq(MessageTemplate::getStatus, MessageConstant.TEMPLATE_STATUS_ENABLED)
                .one();
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
        var wrapper = lambdaQuery();
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
        return wrapper.list();
    }

    @Override
    public boolean existsByCodeExcludingId(String templateCode, Long excludeId) {
        Long count = lambdaQuery()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .ne(excludeId != null, MessageTemplate::getId, excludeId)
                .count();
        return count > 0;
    }
}