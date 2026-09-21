/**
 * @fileoverview 消息 API 模块
 */

import type { ChatSession, RawChatMessage } from '@/types';
import type { RecallPayload } from '@/types/message';
import { request } from './core/request';

export const messageApi = {
    getConversations() {
        return request<ChatSession[]>('/messages/conversations');
    },

    getConversation(userId: string | number) {
        return request<RawChatMessage[]>(`/messages/conversation/${userId}`);
    },

    sendMessage(data: { receiverId: string; content: string }) {
        return request('/messages', {
            method: 'POST',
            body: data,
        });
    },

    markAsRead(ids: (string | number) | (string | number)[]) {
        const idArray = Array.isArray(ids) ? ids : [ids];
        return request('/messages/read', {
            method: 'PUT',
            body: idArray,
        });
    },

    recallMessage(messageId: string): Promise<{ data: RecallPayload }> {
        return request(`/messages/${messageId}/recall`, {
            method: 'PUT',
        });
    },
};
