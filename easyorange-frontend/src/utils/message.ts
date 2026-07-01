/**
 * @fileoverview 消息工具模块
 * @description 提供消息相关的工具函数
 */

import type { ChatMessage, ChatMessageStatus, ChatMessageType, RawChatMessage } from '@/types';

export function normalizeChatMessage(raw: RawChatMessage): ChatMessage {
    return {
        id: raw.id,
        senderId: raw.senderId,
        receiverId: raw.receiverId,
        content: raw.content,
        type: (raw.type as ChatMessageType) ?? 'TEXT',
        status: (raw.status as ChatMessageStatus) ?? 'SENT',
        createTime: raw.createTime,
        readTime: raw.readTime ?? null,
        recalledAt: raw.recalledAt ?? null,
    };
}

export function normalizeChatMessages(raw: RawChatMessage[]): ChatMessage[] {
    return raw.map(normalizeChatMessage);
}
