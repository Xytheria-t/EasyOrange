import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import MessagesPage from './MessagesPage';

const mockGetConversations = vi.hoisted(() => vi.fn());

vi.mock('@/api/messageApi', () => ({
  messageApi: { getConversations: mockGetConversations },
}));

function createMockConversations(count = 2) {
  return Array.from({ length: count }, (_, i) => ({
    id: `conv${i}`,
    targetUserId: `user${i}`,
    targetUserName: `用户${i}`,
    targetUserAvatar: i === 0 ? 'https://example.com/avatar.jpg' : null,
    lastMessage: `最后消息${i}`,
    lastMessageTime: '2026-05-15T10:00:00Z',
    unreadCount: i === 0 ? 3 : 0,
  }));
}

function renderPage() {
  return renderWithProviders(<MessagesPage />);
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('MessagesPage', () => {
  it('renders loading state', () => {
    mockGetConversations.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  it('renders error state', async () => {
    mockGetConversations.mockRejectedValue(new Error('fail'));
    renderPage();
    expect(await screen.findByText('加载消息失败，请稍后重试')).toBeInTheDocument();
  });

  it('renders empty state', async () => {
    mockGetConversations.mockResolvedValue({ data: [] });
    renderPage();
    expect(await screen.findByText('暂无新消息')).toBeInTheDocument();
  });

  it('renders conversation list', async () => {
    mockGetConversations.mockResolvedValue({ data: createMockConversations() });
    renderPage();
    expect(await screen.findByText('用户0')).toBeInTheDocument();
    expect(screen.getByText('用户1')).toBeInTheDocument();
    expect(screen.getByText('最后消息0')).toBeInTheDocument();
    expect(screen.getByText('最后消息1')).toBeInTheDocument();
  });

  it('shows unread badge', async () => {
    mockGetConversations.mockResolvedValue({ data: createMockConversations() });
    renderPage();
    expect(await screen.findByText('3')).toBeInTheDocument();
  });

  it('shows fallback avatar initial', async () => {
    mockGetConversations.mockResolvedValue({ data: createMockConversations() });
    renderPage();
    expect(await screen.findByText('用')).toBeInTheDocument();
  });

  it('contains links to conversation pages', async () => {
    mockGetConversations.mockResolvedValue({ data: createMockConversations() });
    renderPage();
    expect(await screen.findByText('用户0')).toBeInTheDocument();
    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveAttribute('href', '/messages/user0');
  });
});
