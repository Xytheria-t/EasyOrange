package com.cartethyia.easyorange.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.message.constant.MessageConstants;
import com.cartethyia.easyorange.message.dto.vo.MessageTemplateVO;
import com.cartethyia.easyorange.message.entity.MessageTemplate;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.mapper.MessageTemplateMapper;
import com.cartethyia.easyorange.message.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public MessageTemplate getByCode(String templateCode) {
        return getOne(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .eq(MessageTemplate::getStatus, MessageConstants.TEMPLATE_STATUS_ENABLED));
    }

    @Override
    public MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables) {
        MessageTemplate template = getByCode(templateCode);
        BizRequire.notNull(template, MessageResultCode.TEMPLATE_NOT_FOUND);
        String renderedContent = renderContent(template.getContent(), variables);
        String renderedTitle = renderContent(template.getTitle(), variables);

        return MessageTemplateVO.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .templateType(template.getTemplateType())
                .title(renderedTitle)
                .content(renderedContent)
                .variables(template.getVariables())
                .status(template.getStatus())
                .createTime(template.getCreateTime())
                .build();
    }

    @Override
    public String renderContent(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables.getOrDefault(varName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public List<MessageTemplate> selectTemplateList(MessageTemplate template) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        if (template != null) {
            if (template.getTemplateCode() != null) {
                wrapper.like(MessageTemplate::getTemplateCode, template.getTemplateCode());
            }
            if (template.getTemplateName() != null) {
                wrapper.like(MessageTemplate::getTemplateName, template.getTemplateName());
            }
            if (template.getTemplateType() != null) {
                wrapper.eq(MessageTemplate::getTemplateType, template.getTemplateType());
            }
            if (template.getStatus() != null) {
                wrapper.eq(MessageTemplate::getStatus, template.getStatus());
            }
        }
        wrapper.orderByDesc(MessageTemplate::getCreateTime);
        return list(wrapper);
    }

    @Override
    public int insertTemplate(MessageTemplate template) {
        return save(template) ? 1 : 0;
    }

    @Override
    public int updateTemplate(MessageTemplate template) {
        return updateById(template) ? 1 : 0;
    }

    @Override
    public void deleteTemplateByIds(Long[] templateIds) {
        removeBatchByIds(List.of(templateIds));
    }

    @Override
    public boolean checkTemplateCodeUnique(MessageTemplate template) {
        Long count = count(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, template.getTemplateCode())
                .ne(template.getId() != null, MessageTemplate::getId, template.getId()));
        return count == 0;
    }

    @Override
    public void loadingTemplateCache() {
        // TODO: 实现模板缓存加载
        log.debug("loadingTemplateCache invoked - to be implemented");
    }

    @Override
    public void clearTemplateCache() {
        // TODO: 实现模板缓存清除
        log.debug("clearTemplateCache invoked - to be implemented");
    }

    @Override
    public void resetTemplateCache() {
        // TODO: 实现模板缓存重置
        log.debug("resetTemplateCache invoked - to be implemented");
    }
}
