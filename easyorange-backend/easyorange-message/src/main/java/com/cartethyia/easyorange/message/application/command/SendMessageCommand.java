package com.cartethyia.easyorange.message.application.command;

/**
 * 发送消息命令 —— REST 与 WebSocket 两条入口共用的用例入参。传输层校验（@NotBlank / @Size）挂在 inbound
 * 的 Request DTO 上，WS 入口不跑 Bean Validation；会话 ID 不作为入参，由 {@code Message} 聚合根按收发双方算出。
 */
public record SendMessageCommand(String receiverId, Integer type, String title, String content, String businessId) {}
