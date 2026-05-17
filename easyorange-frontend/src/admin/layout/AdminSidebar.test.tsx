import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AdminSidebar } from './AdminSidebar';

const mockLocation = { pathname: '/admin' };
vi.mock('react-router-dom', () => ({
  Link: ({ children, to, ...props }: { children: React.ReactNode; to: string; [key: string]: unknown }) => (
    <a href={to} {...props}>{children}</a>
  ),
  useLocation: () => mockLocation,
}));

const mockAdminStore = vi.fn();
vi.mock('../store', () => ({
  useAdminStore: () => mockAdminStore(),
}));

describe('AdminSidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockLocation.pathname = '/admin';
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: false,
    });
  });

  it('renders logo text', () => {
    render(<AdminSidebar />);
    expect(screen.getByText('易橙管理')).toBeInTheDocument();
  });

  it('renders all nav sections', () => {
    render(<AdminSidebar />);
    expect(screen.getByText('概览')).toBeInTheDocument();
    expect(screen.getByText('管理')).toBeInTheDocument();
    expect(screen.getByText('数据')).toBeInTheDocument();
  });

  it('renders all nav items', () => {
    render(<AdminSidebar />);
    expect(screen.getByText('仪表盘')).toBeInTheDocument();
    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.getByText('商品审核')).toBeInTheDocument();
    expect(screen.getByText('订单管理')).toBeInTheDocument();
    expect(screen.getByText('举报处理')).toBeInTheDocument();
    expect(screen.getByText('数据统计')).toBeInTheDocument();
  });

  it('renders back to main site link', () => {
    render(<AdminSidebar />);
    expect(screen.getByText('返回主站')).toBeInTheDocument();
  });

  it('marks dashboard as active when on /admin', () => {
    render(<AdminSidebar />);
    const dashboardLink = screen.getByText('仪表盘').closest('a');
    expect(dashboardLink).toHaveClass('active');
  });

  it('marks user management as active when on /admin/users', () => {
    mockLocation.pathname = '/admin/users';
    render(<AdminSidebar />);
    const usersLink = screen.getByText('用户管理').closest('a');
    expect(usersLink).toHaveClass('active');
  });

  it('marks nested route as active for both parent and child', () => {
    mockLocation.pathname = '/admin/products/review';
    render(<AdminSidebar />);
    // /admin/products/review matches /admin/products (startsWith) — both active on same item
    const reviewLink = screen.getByText('商品审核').closest('a');
    expect(reviewLink).toHaveClass('active');
  });

  it('does not collapse sections when sidebarCollapsed is false', () => {
    render(<AdminSidebar />);
    expect(screen.getByText('概览')).toBeVisible();
  });

  it('hides section titles when sidebarCollapsed is true', () => {
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: true,
    });
    render(<AdminSidebar />);
    expect(screen.queryByText('概览')).not.toBeInTheDocument();
    expect(screen.queryByText('管理')).not.toBeInTheDocument();
    expect(screen.queryByText('数据')).not.toBeInTheDocument();
  });

  it('applies collapsed class when sidebarCollapsed is true', () => {
    mockAdminStore.mockReturnValue({
      sidebarCollapsed: true,
    });
    const { container } = render(<AdminSidebar />);
    expect(container.querySelector('.admin-sidebar')).toHaveClass('collapsed');
  });
});
