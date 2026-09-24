import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
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

vi.mock('@/components/ui/sheet', () => ({
    Sheet: ({ children }: { children: React.ReactNode }) => <div data-testid="sheet">{children}</div>,
    SheetContent: ({ children }: { children: React.ReactNode }) => <div data-testid="sheet-content">{children}</div>,
    SheetHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
    SheetDescription: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
}));

vi.mock('@/hooks', () => ({
    useMediaQuery: () => false,
}));

vi.mock('react-router-dom', () => ({
    Outlet: () => <div data-testid="outlet">Page Content</div>,
    useLocation: () => ({ pathname: '/admin' }),
}));

const mockAdminStore = vi.fn();
vi.mock('../store', () => ({
    useAdminStore: () => mockAdminStore(),
}));

describe('AdminLayout', () => {
    it('renders all layout components', () => {
        mockAdminStore.mockReturnValue({ sidebarCollapsed: false });
        render(<AdminLayout />);

        expect(screen.getAllByTestId('admin-sidebar').length).toBeGreaterThan(0);
        expect(screen.getByTestId('admin-header')).toBeInTheDocument();
        expect(screen.getByTestId('outlet')).toBeInTheDocument();
        expect(screen.getByTestId('toast-container')).toBeInTheDocument();
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
