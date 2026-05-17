import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AdminHeader } from './AdminHeader';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

const mockAdminStore = vi.fn();
vi.mock('../store', () => ({
  useAdminStore: () => mockAdminStore(),
}));

const mockAuthStore = vi.fn();
vi.mock('@/store', () => ({
  useAuthStore: () => mockAuthStore(),
}));

describe('AdminHeader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock window.location.pathname
    Object.defineProperty(window, 'location', {
      value: { pathname: '/admin' },
      writable: true,
      configurable: true,
    });
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: false,
      toggleSidebar: vi.fn(),
    });
    mockAuthStore.mockReturnValue({
      user: { nickname: 'Admin', username: 'admin' },
    });
  });

  it('renders dashboard title by default', () => {
    render(<AdminHeader />);
    expect(screen.getByText('仪表盘')).toBeInTheDocument();
  });

  it('renders correct title based on pathname', () => {
    window.location = { pathname: '/admin/users' } as Location;
    render(<AdminHeader />);
    expect(screen.getByText('用户管理')).toBeInTheDocument();
  });

  it('shows user name', () => {
    render(<AdminHeader />);
    expect(screen.getByText('Admin')).toBeInTheDocument();
  });

  it('shows user avatar initial', () => {
    render(<AdminHeader />);
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('toggles sidebar on collapse button click', () => {
    const toggleSidebar = vi.fn();
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: false,
      toggleSidebar,
    });
    render(<AdminHeader />);
    fireEvent.click(screen.getByTitle('收起侧边栏'));
    expect(toggleSidebar).toHaveBeenCalledTimes(1);
  });

  it('shows expand title when collapsed', () => {
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: true,
      toggleSidebar: vi.fn(),
    });
    render(<AdminHeader />);
    expect(screen.getByTitle('展开侧边栏')).toBeInTheDocument();
  });

  it('navigates to home when clicking user section', () => {
    render(<AdminHeader />);
    fireEvent.click(screen.getByTitle('返回主站'));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('renders unknown path title', () => {
    window.location.pathname = '/unknown/path';
    render(<AdminHeader />);
    expect(screen.getByText('管理后台')).toBeInTheDocument();
  });
});
