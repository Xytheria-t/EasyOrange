import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AdminLayout } from './AdminLayout';

vi.mock('./AdminSidebar', () => ({
  AdminSidebar: () => <div data-testid="admin-sidebar">Sidebar</div>,
}));

vi.mock('./AdminHeader', () => ({
  AdminHeader: () => <div data-testid="admin-header">Header</div>,
}));

vi.mock('@/components/ui/Toast', () => ({
  ToastContainer: () => <div data-testid="toast-container" />,
}));

vi.mock('@/components/ui/Loading', () => ({
  GlobalLoading: () => <div data-testid="global-loading" />,
}));

vi.mock('react-router-dom', () => ({
  Outlet: () => <div data-testid="outlet">Page Content</div>,
}));

const mockAdminStore = vi.fn();
vi.mock('../store', () => ({
  useAdminStore: () => mockAdminStore(),
}));

describe('AdminLayout', () => {
  it('renders all layout components', () => {
    mockAdminStore.mockReturnValue({ sidebarCollapsed: false });
    render(<AdminLayout />);

    expect(screen.getByTestId('admin-sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('admin-header')).toBeInTheDocument();
    expect(screen.getByTestId('outlet')).toBeInTheDocument();
    expect(screen.getByTestId('toast-container')).toBeInTheDocument();
    expect(screen.getByTestId('global-loading')).toBeInTheDocument();
  });

  it('renders admin root class', () => {
    mockAdminStore.mockReturnValue({ sidebarCollapsed: false });
    const { container } = render(<AdminLayout />);
    expect(container.querySelector('.admin-root')).toBeInTheDocument();
    expect(container.querySelector('.admin-layout')).toBeInTheDocument();
  });

  it('adds sidebar-collapsed class to main when collapsed', () => {
    mockAdminStore.mockReturnValue({ sidebarCollapsed: true });
    render(<AdminLayout />);
    const main = document.querySelector('.admin-content');
    expect(main).toHaveClass('sidebar-collapsed');
  });

  it('does not add sidebar-collapsed class when not collapsed', () => {
    mockAdminStore.mockReturnValue({ sidebarCollapsed: false });
    render(<AdminLayout />);
    const main = document.querySelector('.admin-content');
    expect(main).not.toHaveClass('sidebar-collapsed');
  });
});
