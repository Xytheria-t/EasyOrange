import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/store';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { AdminRouteGuard } from './AdminRouteGuard';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        Navigate: ({ to }: { to: string }) => {
            mockNavigate(to);
            return null;
        },
        Outlet: () => <div>受保护内容</div>,
    };
});

beforeEach(() => {
    useAuthStore.setState({
        user: null,
        token: null,
    });
    mockNavigate.mockClear();
});

describe('AdminRouteGuard', () => {
    it('redirects to login when not authenticated', async () => {
        renderWithProviders(<AdminRouteGuard />, { initialRoute: '/admin/products' });
        // 会话恢复是异步的（restoreSession 的 finally 才置位 sessionChecked），
        // 恢复失败后才判定未登录并跳转 —— 断言要等这条链走完
        await vi.waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login?redirect=%2Fadmin%2Fproducts'), {
            timeout: 5000,
        });
    });

    it('renders protected content when admin', () => {
        useAuthStore.setState({
            token: 'admin-token',
            user: { id: '1', userType: '00' as const, username: 'admin' } as unknown as import('@/types/user').User,
        });

        renderWithProviders(<AdminRouteGuard />);
        expect(screen.getByText('受保护内容')).toBeInTheDocument();
    });
});
