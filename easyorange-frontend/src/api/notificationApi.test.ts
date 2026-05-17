import { describe, it, expect, vi, beforeEach } from 'vitest';
import { notificationApi } from './notificationApi';

const mockRequest = vi.fn();
vi.mock('./core/request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

describe('notificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getNotifications calls request with correct URL, method, and params', () => {
    notificationApi.getNotifications(2, 10);
    expect(mockRequest).toHaveBeenCalledWith('/messages/list', {
      method: 'GET',
      params: { type: 1, pageNum: 2, pageSize: 10 },
    });
  });

  it('getNotifications uses default pageNum=1 and pageSize=20 when no args', () => {
    notificationApi.getNotifications();
    expect(mockRequest).toHaveBeenCalledWith('/messages/list', {
      method: 'GET',
      params: { type: 1, pageNum: 1, pageSize: 20 },
    });
  });

  it('getUnreadCount calls request with correct URL and method', () => {
    notificationApi.getUnreadCount();
    expect(mockRequest).toHaveBeenCalledWith('/messages/unread-count', {
      method: 'GET',
    });
  });

  it('markAsRead calls request with PUT and correct URL including id', () => {
    notificationApi.markAsRead('notif-1');
    expect(mockRequest).toHaveBeenCalledWith('/messages/notif-1/read', {
      method: 'PUT',
    });
  });

  it('markAllSystemAsRead calls request with PUT and correct URL', () => {
    notificationApi.markAllSystemAsRead();
    expect(mockRequest).toHaveBeenCalledWith('/messages/read-by-type/1', {
      method: 'PUT',
    });
  });
});
