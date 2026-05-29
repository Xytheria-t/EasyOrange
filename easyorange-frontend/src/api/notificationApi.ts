import type { PageResult, NotificationItem, UnreadCount } from '@/types';
/**
 * 系统通知 API
 * 
 * 注意：系统通知与即时消息共享同一个 `/api/messages/*` 后端。
 * - getNotifications() 调用 /api/messages/list?type=1（仅系统通知）
 * - 即时消息 API 见 messageApi
 * - markAsRead(id) 标记单条为已读，与 messageApi.markAsRead(ids)（批量标记）不同
 */
import { request } from './core/request';

export const notificationApi = {
  getNotifications(pageNum = 1, pageSize = 20) {
    return request<PageResult<NotificationItem>>('/messages/list', {
      method: 'GET',
      params: { type: 1, pageNum, pageSize }
    });
  },

  getUnreadCount() {
    return request<UnreadCount>('/messages/unread-count', {
      method: 'GET'
    });
  },

  markAsRead(id: string) {
    return request(`/messages/${id}/read`, {
      method: 'PUT'
    });
  },

  markAllSystemAsRead() {
    return request('/messages/read-by-type/1', {
      method: 'PUT'
    });
  }
};
