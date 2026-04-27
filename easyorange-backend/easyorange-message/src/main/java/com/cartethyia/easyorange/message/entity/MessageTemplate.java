package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 消息模板实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_message_template")
public class MessageTemplate extends BaseDO {

    /** 模板编码，唯一标识 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板类型：system, order, chat 等 */
    private String templateType;

    /** 消息标题模板 */
    private String title;

    /** 消息内容模板，支持占位符 */
    private String content;

    /** 模板变量定义，JSON 格式 */
    private String variables;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
