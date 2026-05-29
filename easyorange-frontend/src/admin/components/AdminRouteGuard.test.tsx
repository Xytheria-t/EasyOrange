import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { useAuthStore } from '@/store';
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
    refreshToken: null,
    isAuthenticated: false,
  });
  mockNavigate.mockClear();
});

describe('AdminRouteGuard', () => {
  it('redirects to login when not authenticated', () => {
    renderWithProviders(<AdminRouteGuard />, { initialRoute: '/admin/products' });
    expect(mockNavigate).toHaveBeenCalledWith(
      '/login?redirect=%2Fadmin%2Fproducts',
    );
  });

  it('renders protected content when admin', () => {
    useAuthStore.setState({
      token: 'admin-token',
      isAuthenticated: true,
      user: { id: '1', userType: '00' as const, username: 'admin' } as unknown as import('@/types/user').User,
    });

    renderWithProviders(<AdminRouteGuard />);
    expect(screen.getByText('受保护内容')).toBeInTheDocument();
  });
});
