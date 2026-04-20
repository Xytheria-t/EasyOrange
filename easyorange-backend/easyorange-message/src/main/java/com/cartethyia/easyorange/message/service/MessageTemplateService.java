package com.cartethyia.easyorange.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.message.dto.vo.MessageTemplateVO;
import com.cartethyia.easyorange.message.entity.MessageTemplate;

import java.util.List;
import java.util.Map;

public interface MessageTemplateService extends IService<MessageTemplate> {

    MessageTemplate getByCode(String templateCode);

    MessageTemplateVO renderTemplate(String templateCode, Map<String, String> variables);

    String renderContent(String template, Map<String, String> variables);

    List<MessageTemplate> selectTemplateList(MessageTemplate template);

    int insertTemplate(MessageTemplate template);

    int updateTemplate(MessageTemplate template);

    void deleteTemplateByIds(Long[] templateIds);

    boolean checkTemplateCodeUnique(MessageTemplate template);

    void loadingTemplateCache();

    void clearTemplateCache();

    void resetTemplateCache();
}
