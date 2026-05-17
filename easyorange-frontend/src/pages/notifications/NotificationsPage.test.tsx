import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { NotificationItem } from '@/types';
import NotificationsPage from './NotificationsPage';

// ── Hoisted mock functions ──
const mockGetNotifications = vi.hoisted(() => vi.fn());
const mockMarkAsRead = vi.hoisted(() => vi.fn().mockResolvedValue({}));
const mockMarkAllSystemAsRead = vi.hoisted(() => vi.fn().mockResolvedValue({}));
const mockNavigate = vi.hoisted(() => vi.fn());

// ── Module mocks ──
vi.mock('@/api/notificationApi', () => ({
  notificationApi: {
    getNotifications: mockGetNotifications,
    markAsRead: mockMarkAsRead,
    markAllSystemAsRead: mockMarkAllSystemAsRead,
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

// ── Helpers ──
function createMockNotification(
  overrides: Partial<NotificationItem> = {},
): NotificationItem {
  return {
    id: '1',
    senderId: null,
    senderName: '系统',
    receiverId: 'user1',
    type: 1,
    typeDesc: '系统通知',
    title: '审核通过',
    content: '您的商品已通过审核，现在已上架',
    isRead: 0,
    businessId: 'prod123',
    createTime: '2026-05-16T10:00:00Z',
    updateTime: '2026-05-16T10:00:00Z',
    ...overrides,
  };
}

function renderPage() {
  return renderWithProviders(<NotificationsPage />, {
    initialRoute: '/notifications',
  });
}

beforeEach(() => {
  vi.clearAllMocks();
});

// ── Tests ──
describe('NotificationsPage', () => {
  it('renders the page title "系统通知"', async () => {
    mockGetNotifications.mockResolvedValue({
      data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
    });

    renderPage();

    expect(screen.getByText('系统通知')).toBeInTheDocument();
    expect(screen.getByText('通知中心')).toBeInTheDocument();
  });

  it('shows loading state', () => {
    // Return a promise that never resolves to keep loading
    mockGetNotifications.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  it('shows error state with retry button', async () => {
    mockGetNotifications.mockRejectedValue(new Error('Network error'));

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('加载失败')).toBeInTheDocument();
    });
    expect(screen.getByText('请稍后重试')).toBeInTheDocument();
    expect(screen.getByText('重新加载')).toBeInTheDocument();
  });

  it('shows empty state "暂无系统通知"', async () => {
    mockGetNotifications.mockResolvedValue({
      data: { records: [], total: 0, pages: 1, current: 1, size: 20 },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('暂无系统通知')).toBeInTheDocument();
    });
  });

  it('renders notification list with correct content', async () => {
    const notifications = [
      createMockNotification({
        id: '1',
        title: '审核通过',
        content: '您的商品已通过审核',
        isRead: 0,
        businessId: 'prod123',
      }),
      createMockNotification({
        id: '2',
        title: '审核未通过',
        content: '您的商品未通过审核',
        isRead: 1,
        businessId: 'prod456',
      }),
    ];
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications,
        total: 2,
        pages: 1,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('审核通过')).toBeInTheDocument();
    });
    expect(screen.getByText('您的商品已通过审核')).toBeInTheDocument();
    expect(screen.getByText('审核未通过')).toBeInTheDocument();
    expect(screen.getByText('您的商品未通过审核')).toBeInTheDocument();
  });

  it('shows "全部已读" button when there are unread notifications', async () => {
    const notifications = [
      createMockNotification({ id: '1', title: '审核通过', isRead: 0 }),
      createMockNotification({ id: '2', title: '系统通知', isRead: 1 }),
    ];
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications,
        total: 2,
        pages: 1,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('全部已读')).toBeInTheDocument();
    });
  });

  it('hides "全部已读" button when all notifications are read', async () => {
    const notifications = [
      createMockNotification({ id: '1', title: '审核通过', isRead: 1 }),
    ];
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications,
        total: 1,
        pages: 1,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('审核通过')).toBeInTheDocument();
    });
    expect(screen.queryByText('全部已读')).not.toBeInTheDocument();
  });

  it('marks notification as read and navigates on click', async () => {
    const notifications = [
      createMockNotification({
        id: '1',
        title: '审核通过',
        isRead: 0,
        businessId: 'prod123',
      }),
    ];
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications,
        total: 1,
        pages: 1,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('审核通过')).toBeInTheDocument();
    });

    // Click the notification card
    const notificationCard = screen.getByText('审核通过').closest('button');
    expect(notificationCard).not.toBeNull();
    await user.click(notificationCard!);

    // Should mark as read
    expect(mockMarkAsRead).toHaveBeenCalledWith('1');
    // Should navigate to product detail
    expect(mockNavigate).toHaveBeenCalledWith('/products/prod123');
  });

  it('does not mark as read if notification is already read', async () => {
    const notifications = [
      createMockNotification({
        id: '1',
        title: '审核通过',
        isRead: 1,
        businessId: 'prod123',
      }),
    ];
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications,
        total: 1,
        pages: 1,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    const user = userEvent.setup();

    await waitFor(() => {
      expect(screen.getByText('审核通过')).toBeInTheDocument();
    });

    const notificationCard = screen.getByText('审核通过').closest('button');
    expect(notificationCard).not.toBeNull();
    await user.click(notificationCard!);

    // Already read, so markAsRead should NOT be called
    expect(mockMarkAsRead).not.toHaveBeenCalled();
    // But navigation should still happen
    expect(mockNavigate).toHaveBeenCalledWith('/products/prod123');
  });

  it('shows pagination when there are multiple pages', async () => {
    // Create 21 notifications (more than one page of 20)
    const notifications = Array.from({ length: 21 }, (_, i) =>
      createMockNotification({
        id: String(i + 1),
        title: `通知 ${i + 1}`,
        isRead: 1,
      }),
    );
    mockGetNotifications.mockResolvedValue({
      data: {
        records: notifications.slice(0, 20),
        total: 21,
        pages: 2,
        current: 1,
        size: 20,
      },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('1 / 2')).toBeInTheDocument();
    });
    expect(screen.getByText('上一页')).toBeInTheDocument();
    expect(screen.getByText('下一页')).toBeInTheDocument();
  });
});
