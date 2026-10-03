/**
 * @fileoverview 消息工具模块
 * @description 提供消息相关的工具函数
 */

import type { ChatMessage, ChatMessageStatus, RawChatMessage } from '@/types';

/** 后端 MessageType code（{@code eo_message.type} TINYINT）→ 前端类型名 */
const MESSAGE_TYPE_BY_CODE: Record<number, ChatMessage['type']> = {
    1: 'TEXT',
    2: 'TEXT',
    3: 'TEXT',
    4: 'TEXT',
    5: 'TEXT',
};

/**
 * 状态合成 —— 撤回优先于已读。
 *
 * <p>后端 {@code MessageStatus} 只有 SENT / RECALLED 两个取值，「已读」由 `is_read` 列单独承载
 * （两者不交叉，见后端 ReadStatus 的注释）。此前这里直接把 `status` 当已读态用，而它恒为 SENT，
 * 结果自己发的消息永远只显示单勾，对方的未读消息每次列表刷新都被重发一次标已读请求。
 */
function resolveStatus(raw: RawChatMessage): ChatMessageStatus {
    if (raw.status === 'RECALLED') {
        return 'RECALLED';
    }
    return raw.isRead === 1 ? 'READ' : 'SENT';
}

export function normalizeChatMessage(raw: RawChatMessage): ChatMessage {
    return {
        id: raw.id,
        senderId: raw.senderId,
        senderAvatar: raw.senderAvatar ?? null,
        receiverId: raw.receiverId,
        content: raw.content,
        title: raw.title ?? null,
        type: (raw.type != null && MESSAGE_TYPE_BY_CODE[raw.type]) || 'TEXT',
        status: resolveStatus(raw),
        createTime: raw.createTime,
        readTime: raw.readTime ?? null,
        recalledAt: raw.recalledAt ?? null,
    };
}

export function normalizeChatMessages(raw: RawChatMessage[]): ChatMessage[] {
    return raw.map(normalizeChatMessage);
}
