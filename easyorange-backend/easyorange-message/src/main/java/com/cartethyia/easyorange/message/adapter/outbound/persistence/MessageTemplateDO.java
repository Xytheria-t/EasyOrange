package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message_template")
public class MessageTemplateDO extends BaseDO {

    private String templateCode;
    private String templateName;
    private String templateType;
    private String title;
    private String content;
    private String variables;
    private Integer status;
    private String remark;

    public String getTemplateCode() { return templateCode; }
    public String getTemplateName() { return templateName; }
    public String getTemplateType() { return templateType; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getVariables() { return variables; }
    public Integer getStatus() { return status; }
    public String getRemark() { return remark; }
}
