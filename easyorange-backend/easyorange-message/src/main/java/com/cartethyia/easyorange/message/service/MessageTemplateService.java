package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplate;
import com.cartethyia.easyorange.message.application.query.dto.MessageTemplateVO;

import java.util.List;
import java.util.Map;

public interface MessageTemplateService {

    MessageTemplate getByCode(String templateCode);

    MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables);

    String renderContent(String template, Map<String, String> variables);

    List<MessageTemplate> selectTemplateList(MessageTemplate condition);

    void insertTemplate(MessageTemplate template);

    void updateTemplate(MessageTemplate template);

    void deleteTemplateByIds(String[] templateIds);

    boolean checkTemplateCodeUnique(MessageTemplate template);

    void loadingTemplateCache();

    void clearTemplateCache();

    void resetTemplateCache();
}
