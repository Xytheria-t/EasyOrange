package com.cartethyia.easyorange.message.service.impl;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.message.domain.repository.MessageTemplateRepository;
import com.cartethyia.easyorange.message.dto.vo.MessageTemplateVO;
import com.cartethyia.easyorange.message.entity.MessageTemplate;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import com.cartethyia.easyorange.message.service.MessageTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final MessageTemplateRepository messageTemplateRepository;

    @Override
    public MessageTemplate getByCode(String templateCode) {
        return messageTemplateRepository.findByCode(templateCode);
    }

    @Override
    public MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables) {
        MessageTemplate template = messageTemplateRepository.findByCode(templateCode);
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
        return messageTemplateRepository.findByCondition(template);
    }

    @Override
    public int insertTemplate(MessageTemplate template) {
        messageTemplateRepository.save(template);
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTemplate(MessageTemplate template) {
        messageTemplateRepository.update(template);
        return 1;
    }

    @Override
    public void deleteTemplateByIds(Long[] templateIds) {
        messageTemplateRepository.deleteByIds(templateIds);
    }

    @Override
    public boolean checkTemplateCodeUnique(MessageTemplate template) {
        return !messageTemplateRepository.existsByCodeExcludingId(template.getTemplateCode(), template.getId());
    }

    @Override
    public void loadingTemplateCache() {
        log.debug("loadingTemplateCache invoked - to be implemented");
    }

    @Override
    public void clearTemplateCache() {
        log.debug("clearTemplateCache invoked - to be implemented");
    }

    @Override
    public void resetTemplateCache() {
        log.debug("resetTemplateCache invoked - to be implemented");
    }
}
