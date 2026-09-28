package com.cartethyia.easyorange.message.application.command;

/**
 * 发送消息命令 —— REST 与 WebSocket 两条入口共用的用例入参。
 * <p>
 * 取舍：传输层校验（@NotBlank / @Size）挂在 inbound 的 Request DTO 上，命令只承载已过边界校验的值——
 * WS 入口不跑 Bean Validation，注解留在这里不生效，写上去只会让人误以为这条链有校验。
 * <p>
 * 边界：会话 ID 不作为入参，由 {@code Message} 聚合根按收发双方算出（客户端给的房间名不可信）。
 */
public record SendMessageCommand(String receiverId, Integer type, String title, String content, String businessId) {}
