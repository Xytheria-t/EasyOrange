import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { ProfileSidebar } from './ProfileSidebar';
import type { User } from '@/types';

// ─── Mocks ──────────────────────────────────────────────────────────────
const { mockNavigate, mockInvalidateQueries, mockAddToast, mockUploadAvatar } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  mockInvalidateQueries: vi.fn(),
  mockAddToast: vi.fn(),
  mockUploadAvatar: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
}));

vi.mock('@/store/uiStore', () => ({
  useUIStore: (selector: (s: Record<string, unknown>) => unknown) => {
    const store = { addToast: mockAddToast };
    return selector ? selector(store) : store;
  },
}));

vi.mock('@/api/userApi', () => ({
  userApi: { uploadAvatar: mockUploadAvatar },
}));

vi.mock('@/utils/errorHandler', () => ({
  errorHandler: { handle: vi.fn() },
}));

vi.mock('lucide-react', () => {
  const icons: Record<string, React.ComponentType<Record<string, unknown>>> = {};
  const names = [
    'Camera', 'LogOut', 'Package', 'Sparkles',
    'ShoppingBag', 'List', 'Activity', 'Shield', 'Settings',
    'Award',
  ];
  names.forEach((name) => {
    icons[name] = (props) =>
      React.createElement('svg', {
        'data-testid': `icon-${name.toLowerCase()}`,
        ...props,
      });
  });
  return icons;
});

// ─── Test Data ──────────────────────────────────────────────────────────
const mockUser: User = {
  userId: '1',
  username: 'testuser',
  nickname: 'TestUser',
  email: 'test@example.com',
  phone: '13800138000',
  studentId: '2021001',
  realName: 'Test',
  avatar: null,
  status: 1,
  userType: '00',
  createTime: '2026-01-01T00:00:00Z',
  updateTime: '2026-01-01T00:00:00Z',
};

const defaultProps = {
  user: mockUser,
  activeTab: 'overview' as const,
  onTabChange: vi.fn(),
  onLogout: vi.fn(),
  animateIn: true,
};

// ─── Tests ──────────────────────────────────────────────────────────────
describe('ProfileSidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.scrollTo = vi.fn();
  });

  it('renders sidebar with testid "profile-sidebar"', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByTestId('profile-sidebar')).toBeInTheDocument();
  });

  it('shows user nickname when provided', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('TestUser')).toBeInTheDocument();
  });

  it('shows username with @ prefix', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('@testuser')).toBeInTheDocument();
  });

  it('shows avatar fallback with first letter when no avatar', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('T')).toBeInTheDocument();
  });

  it('shows avatar image when user.avatar is provided', () => {
    const userWithAvatar: User = { ...mockUser, avatar: 'https://example.com/avatar.jpg' };
    render(<ProfileSidebar {...defaultProps} user={userWithAvatar} />);
    const img = screen.getByAltText('头像') as HTMLImageElement;
    expect(img).toBeInTheDocument();
    expect(img.src).toBe('https://example.com/avatar.jpg');
  });

  it('renders all 4 nav items (总览, 动态, 安全, 偏好)', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('总览')).toBeInTheDocument();
    expect(screen.getByText('动态')).toBeInTheDocument();
    expect(screen.getByText('安全')).toBeInTheDocument();
    expect(screen.getByText('偏好')).toBeInTheDocument();
  });

  it('highlights active tab', () => {
    render(<ProfileSidebar {...defaultProps} />);
    const activeButton = screen.getByText('总览').closest('button');
    expect(activeButton?.className).toContain('active');
  });

  it('calls onTabChange when nav item clicked', () => {
    const onTabChange = vi.fn();
    render(<ProfileSidebar {...defaultProps} onTabChange={onTabChange} />);
    fireEvent.click(screen.getByText('动态'));
    expect(onTabChange).toHaveBeenCalledWith('activity');
  });

  it('shows credit score (98) and transaction count (42)', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('98')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('renders action buttons (发布商品, 我的发布, 购买记录, 我的信用, 退出登录)', () => {
    render(<ProfileSidebar {...defaultProps} />);
    expect(screen.getByText('发布商品')).toBeInTheDocument();
    expect(screen.getByText('我的发布')).toBeInTheDocument();
    expect(screen.getByText('购买记录')).toBeInTheDocument();
    expect(screen.getByText('我的信用')).toBeInTheDocument();
    expect(screen.getByText('退出登录')).toBeInTheDocument();
  });

  it('navigates to /publish on publish button click', () => {
    render(<ProfileSidebar {...defaultProps} />);
    fireEvent.click(screen.getByText('发布商品'));
    expect(mockNavigate).toHaveBeenCalledWith('/publish');
  });

  it('navigates to /my-products on my products button click', () => {
    render(<ProfileSidebar {...defaultProps} />);
    fireEvent.click(screen.getByText('我的发布'));
    expect(mockNavigate).toHaveBeenCalledWith('/my-products');
  });

  it('calls onLogout on logout button click', () => {
    const onLogout = vi.fn();
    render(<ProfileSidebar {...defaultProps} onLogout={onLogout} />);
    fireEvent.click(screen.getByTestId('btn-profile-logout'));
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it('handles undefined user gracefully (shows "U" as avatar fallback)', () => {
    render(<ProfileSidebar {...defaultProps} user={undefined} />);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('handles undefined user gracefully and defaults to "用户" for display name', () => {
    render(<ProfileSidebar {...defaultProps} user={undefined} />);
    expect(screen.getByText('用户')).toBeInTheDocument();
    expect(screen.getByText('@unknown')).toBeInTheDocument();
  });

  it('calls handleAvatarClick on avatar button click (opens file input)', () => {
    render(<ProfileSidebar {...defaultProps} />);
    const avatarButton = screen.getByLabelText('更换头像');
    fireEvent.click(avatarButton);
    // Verify the button is clickable and the hidden file input exists
    const fileInput = document.querySelector('input[type="file"]');
    expect(fileInput).toBeInTheDocument();
    expect(fileInput).toHaveAttribute('accept', 'image/*');
  });

  it('shows animate-in class when animateIn is true', () => {
    render(<ProfileSidebar {...defaultProps} animateIn={true} />);
    const sidebar = screen.getByTestId('profile-sidebar');
    expect(sidebar.className).toContain('animate-in');
  });

  it('does not show animate-in class when animateIn is false', () => {
    render(<ProfileSidebar {...defaultProps} animateIn={false} />);
    const sidebar = screen.getByTestId('profile-sidebar');
    expect(sidebar.className).not.toContain('animate-in');
  });
});
