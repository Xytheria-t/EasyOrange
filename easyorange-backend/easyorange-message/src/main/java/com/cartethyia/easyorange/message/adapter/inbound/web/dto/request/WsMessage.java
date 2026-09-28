package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STOMP 入参（{@code /app/chat.send} 与 {@code /app/chat.typing} 共用）。
 * <p>
 * 只收客户端真正有权决定的值：发送方身份取自 Principal、消息 id 与 createTime 由服务端生成，
 * 因此这几个字段不在入参形状里——留着会让人以为客户端能指定消息身份。
 * <p>
 * 用 Lombok {@code @Data} 而非 record：STOMP 帧经消息转换器绑定，record 的构造器绑定在此链路不生效
 * （本项目「请求 DTO 一律 record」的既定例外，与 {@code PageRequest} 子类同款理由）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsMessage {

    private String receiverId;

    private Integer type;

    private String title;

    private String content;

    private String businessId;

    private String conversationId;

    private String targetUserId;
}
