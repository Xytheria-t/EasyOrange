package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import com.cartethyia.easyorange.message.application.query.dto.MessageTemplateVO;

import java.util.List;
import java.util.Map;

public interface MessageTemplateService {

    MessageTemplateAggregate getByCode(String templateCode);

    MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables);

    String renderContent(String template, Map<String, String> variables);

    List<MessageTemplateAggregate> selectTemplateList(MessageTemplateAggregate condition);

    void insertTemplate(MessageTemplateAggregate template);

    void updateTemplate(MessageTemplateAggregate template);

    void deleteTemplateByIds(Long[] templateIds);

    boolean checkTemplateCodeUnique(MessageTemplateAggregate template);

    void loadingTemplateCache();

    void clearTemplateCache();

    void resetTemplateCache();
}
