import { describe, it, expect, vi, beforeEach } from 'vitest';
import { messageApi } from './messageApi';

const mockRequest = vi.fn();
vi.mock('./core/request', () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

describe('messageApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getList calls request with correct path', () => {
    messageApi.getMessages();
    expect(mockRequest).toHaveBeenCalledWith('/messages/list', { method: 'GET' });
  });

  it('getConversations calls request with correct path', () => {
    messageApi.getConversations();
    expect(mockRequest).toHaveBeenCalledWith('/messages/conversations');
  });

  it('getConversation calls request with userId', () => {
    messageApi.getConversation(123);
    expect(mockRequest).toHaveBeenCalledWith('/messages/conversation/123');
  });

  it('sendMessage calls request with POST and body', () => {
    const data = { receiverId: '456', content: 'Hello' };
    messageApi.sendMessage(data);
    expect(mockRequest).toHaveBeenCalledWith('/messages', {
      method: 'POST',
      body: data,
    });
  });

  it('markAsRead calls request with PUT and single id as array', () => {
    messageApi.markAsRead(789);
    expect(mockRequest).toHaveBeenCalledWith('/messages/read', {
      method: 'PUT',
      body: [789],
    });
  });

  it('markAsRead calls request with PUT and array of ids', () => {
    messageApi.markAsRead([1, 2, 3]);
    expect(mockRequest).toHaveBeenCalledWith('/messages/read', {
      method: 'PUT',
      body: [1, 2, 3],
    });
  });

  it('delete calls request with DELETE', () => {
    messageApi.deleteMessage('msg-1');
    expect(mockRequest).toHaveBeenCalledWith('/messages/msg-1', {
      method: 'DELETE',
    });
  });

  it('typing calls request with POST and body', () => {
    const data = { conversationId: 'conv-1', targetUserId: 'user-1' };
    messageApi.typing(data);
    expect(mockRequest).toHaveBeenCalledWith('/messages/typing', {
      method: 'POST',
      body: data,
    });
  });

  it('recall calls request with PUT', () => {
    messageApi.recallMessage('msg-1');
    expect(mockRequest).toHaveBeenCalledWith('/messages/msg-1/recall', {
      method: 'PUT',
    });
  });
});
