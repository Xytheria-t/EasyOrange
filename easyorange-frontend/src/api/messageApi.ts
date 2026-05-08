/**
 * @fileoverview 消息 API 模块
 */

import type { PageResult, ChatMessage } from '@/types';
import { request } from './core/request';

export const messageApi = {
    getList(_params?: Record<string, unknown>) {
        return request<PageResult<Record<string, unknown>>>('/messages/list', {
            method: 'GET'
        });
    },

    getConversations() {
        return request<Record<string, unknown>[]>('/messages/conversations');
    },

    getConversation(userId: string | number) {
        return request<ChatMessage[]>(`/messages/conversation/${userId}`);
    },

    sendMessage(data: { receiverId: number; content: string }) {
        return request('/messages', {
            method: 'POST',
            body: data
        });
    },

    markAsRead(ids: number | number[]) {
        const idArray = Array.isArray(ids) ? ids : [ids];
        return request('/messages/read', {
            method: 'PUT',
            body: idArray
        });
    },

    delete(id: number) {
        return request(`/messages/${id}`, {
            method: 'DELETE'
        });
    }
};
