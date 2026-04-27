package com.cartethyia.easyorange.message.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageTemplateVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String title;

    private String content;

    private String variables;

    private Integer status;

    private LocalDateTime createTime;
}
