package com.cartethyia.easyorange.user.domain.event;

/**
 * 密码变更事件 — 密码一经改写，此前签发的会话全部作废。
 * <p>
 * {@code source} 区分改密来源（"self" 自助改密 / "sms" 验证码找回 / "admin" 管理端重置），
 * 下游据此决定告警级别：自助改密属常态，{@code admin} 重置多为账号被盗后的止损动作。
 *
 * @param eventId 事件唯一 ID（UUID v7），消费端幂等去重键
 * @param userId  密码所属用户 ID
 * @param source  改密来源：self / sms / admin
 */
public record UserPasswordChangedEvent(String eventId, String userId, String source) implements UserEvent {}
