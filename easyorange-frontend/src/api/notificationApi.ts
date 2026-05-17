import type { PageResult, NotificationItem, UnreadCount } from '@/types';
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
