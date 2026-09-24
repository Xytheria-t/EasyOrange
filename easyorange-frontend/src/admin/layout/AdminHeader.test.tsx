import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
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

const mockAddToast = vi.fn();
vi.mock('@/store/uiStore', () => ({
    useUIStore: (selector: (s: { addToast: typeof mockAddToast }) => unknown) => selector({ addToast: mockAddToast }),
}));

const mockUseMediaQuery = vi.fn(() => true);
const mockLogout = vi.fn(async () => {});
vi.mock('@/hooks', () => ({
    useMediaQuery: () => mockUseMediaQuery(),
    useLogout: () => mockLogout,
}));

describe('AdminHeader', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockUseMediaQuery.mockReturnValue(true);
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
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('数据统计')).toBeInTheDocument();
    });

    it('renders correct title based on pathname', () => {
        window.location.pathname = '/admin/users';
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('用户管理')).toBeInTheDocument();
    });

    it('shows user name', () => {
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('Admin')).toBeInTheDocument();
    });

    it('shows user avatar initial', () => {
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('A')).toBeInTheDocument();
    });

    it('toggles sidebar on collapse button click', () => {
        const toggleSidebar = vi.fn();
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: false,
            toggleSidebar,
        });
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('收起侧边栏'));
        expect(toggleSidebar).toHaveBeenCalledTimes(1);
    });

    it('shows expand title when collapsed', () => {
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: true,
            toggleSidebar: vi.fn(),
        });
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByTitle('展开侧边栏')).toBeInTheDocument();
    });

    it('opens mobile nav instead of toggling on narrow viewport', () => {
        mockUseMediaQuery.mockReturnValue(false);
        const toggleSidebar = vi.fn();
        const onOpenMobileNav = vi.fn();
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: false,
            toggleSidebar,
        });
        render(<AdminHeader onOpenMobileNav={onOpenMobileNav} />);
        fireEvent.click(screen.getByTitle('打开导航菜单'));
        expect(onOpenMobileNav).toHaveBeenCalledTimes(1);
        expect(toggleSidebar).not.toHaveBeenCalled();
    });

    it('点击用户区块回到主站，但不结束会话', () => {
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('返回主站'));
        expect(mockNavigate).toHaveBeenCalledWith('/');
        expect(mockLogout).not.toHaveBeenCalled();
    });

    it('点击退出登录 -> 登出、提示、落登录页', async () => {
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('退出登录'));
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });
        expect(mockLogout).toHaveBeenCalledTimes(1);
        expect(mockAddToast).toHaveBeenCalledWith({ type: 'success', message: '已退出登录' });
    });

    it('renders unknown path title', () => {
        window.location.pathname = '/unknown/path';
        render(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('管理后台')).toBeInTheDocument();
    });
});
