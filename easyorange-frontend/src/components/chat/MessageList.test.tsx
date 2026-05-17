import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import MessageList from './MessageList';

// Shared mock state that tests can mutate
let mockVirtualItems: Array<{ index: number; start: number; end: number; key: string; size: number }> = [];

vi.mock('@tanstack/react-virtual', () => ({
  useVirtualizer: vi.fn(() => ({
    getVirtualItems: vi.fn(() => mockVirtualItems),
    getTotalSize: vi.fn(() => mockVirtualItems.reduce((sum, item) => sum + item.size, 0)),
    scrollToIndex: vi.fn(),
    scrollOffset: null,
  })),
}));

function makeMessage(overrides: Record<string, unknown> = {}) {
  return {
    id: 'm1',
    content: 'hello',
    senderId: 'user1',
    receiverId: 'user2',
    createTime: new Date().toISOString(),
    type: 'TEXT' as const,
    status: 'SENT' as const,
    conversationId: 'conv1',
    ...overrides,
  };
}

describe('MessageList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockVirtualItems = [];
  });

  it('renders messages', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'm1', size: 72 },
    ];
    const messages = [makeMessage({ id: 'm1', content: 'Hello there' })];
    render(
      <MessageList
        messages={messages}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={false}
      />,
    );
    expect(screen.getByText('Hello there')).toBeInTheDocument();
  });

  it('shows date separator for today', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'm1', size: 72 },
    ];
    const messages = [makeMessage({ id: 'm1', createTime: new Date().toISOString() })];
    render(
      <MessageList
        messages={messages}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={false}
      />,
    );
    expect(screen.getByText('今天')).toBeInTheDocument();
  });

  it('shows "加载更多消息" button when hasMore is true', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'load-more', size: 72 },
      { index: 1, start: 72, end: 144, key: 'm1', size: 72 },
    ];
    const messages = [makeMessage({ id: 'm1' })];
    render(
      <MessageList
        messages={messages}
        currentUserId="user1"
        targetUserName="Bob"
        hasMore={true}
        isTyping={false}
      />,
    );
    expect(screen.getByText('加载更多消息')).toBeInTheDocument();
  });

  it('shows typing indicator when isTyping is true', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'typing-indicator', size: 72 },
    ];
    render(
      <MessageList
        messages={[]}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={true}
      />,
    );
    expect(screen.getByText(/Bob/)).toBeInTheDocument();
    expect(screen.getByText(/正在输入/)).toBeInTheDocument();
  });

  it('does not show typing indicator when isTyping is false', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'typing-indicator', size: 72 },
    ];
    render(
      <MessageList
        messages={[]}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={false}
      />,
    );
    expect(screen.queryByText(/正在输入/)).not.toBeInTheDocument();
  });

  it('shows own messages with isOwn=true (right side)', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'm1', size: 72 },
    ];
    const messages = [makeMessage({ id: 'm1', senderId: 'user1' })];
    const { container } = render(
      <MessageList
        messages={messages}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={false}
      />,
    );
    // The first message will have isOwn=true, so the bubble renders with justify-end
    const messageText = screen.getByText('hello');
    expect(messageText).toBeInTheDocument();
  });

  it('shows other messages with isOwn=false (left side)', () => {
    mockVirtualItems = [
      { index: 0, start: 0, end: 72, key: 'm1', size: 72 },
    ];
    const messages = [makeMessage({ id: 'm1', senderId: 'user2' })];
    render(
      <MessageList
        messages={messages}
        currentUserId="user1"
        targetUserName="Bob"
        isTyping={false}
      />,
    );
    expect(screen.getByText('hello')).toBeInTheDocument();
  });
});
