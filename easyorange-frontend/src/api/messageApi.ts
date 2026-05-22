/**
 * @fileoverview 消息 API 模块
 */

import type { PageResult, ChatSession, ChatMessage } from '@/types';
import type { RecallPayload } from '@/types/message';
import { request } from './core/request';

export const messageApi = {
  getMessages() {
    return request<PageResult<Record<string, unknown>>>('/messages/list', {
      method: 'GET'
    });
  },

  getConversations() {
    return request<ChatSession[]>('/messages/conversations');
  },

  getConversation(userId: string | number) {
    return request<ChatMessage[]>(`/messages/conversation/${userId}`);
  },

  sendMessage(data: { receiverId: string; content: string }) {
    return request('/messages', {
      method: 'POST',
      body: data
    });
  },

  markAsRead(ids: (string | number) | (string | number)[]) {
    const idArray = Array.isArray(ids) ? ids : [ids];
    return request('/messages/read', {
      method: 'PUT',
      body: idArray
    });
  },

  deleteMessage(id: string) {
    return request(`/messages/${id}`, {
      method: 'DELETE'
    });
  },

  typing(data: { conversationId: string; targetUserId: string }) {
    return request('/messages/typing', {
      method: 'POST',
      body: data
    });
  },

  recallMessage(messageId: string): Promise<{ data: RecallPayload }> {
    return request(`/messages/${messageId}/recall`, {
      method: 'PUT'
    });
  }
};
