package com.cartethyia.easyorange.user.domain.event;

/** 密码变更事件 — 改密后此前签发的会话全部作废；{@code source}（self/sms/admin）供下游定告警级别。 */
public record UserPasswordChangedEvent(String eventId, String userId, String source) implements UserEvent {}
