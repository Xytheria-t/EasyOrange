package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import org.springframework.data.redis.core.RedisTemplate;
import com.cartethyia.easyorange.message.domain.repository.MessageTemplateRepository;
import com.cartethyia.easyorange.message.application.query.dto.MessageTemplateVO;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final String TEMPLATE_CACHE_KEY = "eo:message:templates";

    private final MessageTemplateRepository messageTemplateRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public MessageTemplateAggregate getByCode(String templateCode) {
        return messageTemplateRepository.findByCode(templateCode);
    }

    @Override
    public MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables) {
        MessageTemplateAggregate template = messageTemplateRepository.findByCode(templateCode);
        BizRequire.notNull(template, MessageResultCode.TEMPLATE_NOT_FOUND);
        String renderedContent = renderContent(template.content(), variables);
        String renderedTitle = renderContent(template.title(), variables);

        return MessageTemplateVO.builder()
                .id(template.id())
                .templateCode(template.templateCode())
                .templateName(template.templateName())
                .templateType(template.templateType())
                .title(renderedTitle)
                .content(renderedContent)
                .variables(template.variables())
                .status(template.status())
                .build();
    }

    @Override
    public String renderContent(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        var result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables.getOrDefault(varName, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public List<MessageTemplateAggregate> selectTemplateList(MessageTemplateAggregate condition) {
        return messageTemplateRepository.findByCondition(condition);
    }

    @Override
    public void insertTemplate(MessageTemplateAggregate template) {
        messageTemplateRepository.save(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(MessageTemplateAggregate template) {
        messageTemplateRepository.update(template);
    }

    @Override
    public void deleteTemplateByIds(String[] templateIds) {
        messageTemplateRepository.deleteByIds(templateIds);
    }

    @Override
    public boolean checkTemplateCodeUnique(MessageTemplateAggregate template) {
        return !messageTemplateRepository.existsByCodeExcludingId(template.templateCode(), template.id());
    }

    @Override
    public void loadingTemplateCache() {
        log.info("开始加载消息模板缓存");
        List<MessageTemplateAggregate> templates = messageTemplateRepository.findByCondition(null);
        Map<String, MessageTemplateAggregate> templateMap = new HashMap<>();
        for (MessageTemplateAggregate template : templates) {
            templateMap.put(template.templateCode(), template);
        }
        if (!templateMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(TEMPLATE_CACHE_KEY, templateMap);
        }
        log.info("消息模板缓存加载完成，共 {} 条", templateMap.size());
    }

    @Override
    public void clearTemplateCache() {
        log.info("清除消息模板缓存");
        redisTemplate.delete(TEMPLATE_CACHE_KEY);
    }

    @Override
    public void resetTemplateCache() {
        log.info("重置消息模板缓存");
        clearTemplateCache();
        loadingTemplateCache();
    }
}
