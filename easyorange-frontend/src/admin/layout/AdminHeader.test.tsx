import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactElement } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminHeader } from './AdminHeader';

// 顶栏的「刷新数据」用 useQueryClient / useIsFetching，必须有 QueryClientProvider；
// 这里不能用 renderWithProviders——本文件把 react-router-dom 整个 mock 掉了
function renderHeader(ui: ReactElement) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

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
            user: { nickname: 'Admin', username: 'admin', userType: '00' },
        });
    });

    // 顶栏不再重复页面标题：标题只有各页页头一处（/admin/stats 深链曾回落成「管理后台」）
    it('does not render a duplicated page title', () => {
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.queryByText('数据统计')).not.toBeInTheDocument();
    });

    it('shows the real role from userType instead of a hardcoded one', () => {
        mockAuthStore.mockReturnValue({
            user: { nickname: 'Admin', username: 'admin', userType: '02' },
        });
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('管理员')).toBeInTheDocument();
        expect(screen.queryByText('超级管理员')).not.toBeInTheDocument();
    });

    it('shows user name', () => {
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('Admin')).toBeInTheDocument();
    });

    it('shows user avatar initial', () => {
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        expect(screen.getByText('A')).toBeInTheDocument();
    });

    it('toggles sidebar on collapse button click', () => {
        const toggleSidebar = vi.fn();
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: false,
            toggleSidebar,
        });
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('收起侧边栏'));
        expect(toggleSidebar).toHaveBeenCalledTimes(1);
    });

    it('shows expand title when collapsed', () => {
        mockAdminStore.mockReturnValue({
            sidebarCollapsed: true,
            toggleSidebar: vi.fn(),
        });
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
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
        renderHeader(<AdminHeader onOpenMobileNav={onOpenMobileNav} />);
        fireEvent.click(screen.getByTitle('打开导航菜单'));
        expect(onOpenMobileNav).toHaveBeenCalledTimes(1);
        expect(toggleSidebar).not.toHaveBeenCalled();
    });

    it('点击用户区块回到主站，但不结束会话', () => {
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('返回主站'));
        expect(mockNavigate).toHaveBeenCalledWith('/');
        expect(mockLogout).not.toHaveBeenCalled();
    });

    it('点击退出登录 -> 登出、提示、落登录页', async () => {
        renderHeader(<AdminHeader onOpenMobileNav={vi.fn()} />);
        fireEvent.click(screen.getByTitle('退出登录'));
        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });
        expect(mockLogout).toHaveBeenCalledTimes(1);
        expect(mockAddToast).toHaveBeenCalledWith({ type: 'success', message: '已退出登录' });
    });

    // 数据新鲜度入口：管理端读的是实时值，顶栏必须有一个全局刷新
    it('提供全局刷新入口，一个 admin 前缀刷全站', () => {
        const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
        const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
        render(
            <QueryClientProvider client={queryClient}>
                <AdminHeader onOpenMobileNav={vi.fn()} />
            </QueryClientProvider>
        );
        fireEvent.click(screen.getByTitle('刷新全部管理端数据'));
        expect(invalidate).toHaveBeenCalledWith({ queryKey: ['admin'] });
    });
});
