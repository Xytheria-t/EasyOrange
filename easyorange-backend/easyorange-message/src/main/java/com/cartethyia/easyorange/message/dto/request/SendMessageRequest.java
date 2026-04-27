package com.cartethyia.easyorange.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "接收者 ID 不能为空")
    private Long receiverId;

    @NotNull(message = "消息类型不能为空")
    private Integer type;

    @NotBlank(message = "消息标题不能为空")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private Long businessId;
}
