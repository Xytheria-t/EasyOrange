import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { Header } from './Header';

// Mock NotificationBell since it uses React Query hooks
vi.mock('@/components/notification/NotificationBell', () => ({
  NotificationBell: () => <div data-testid="notification-bell" />,
}));

// Mock stores
const mockAuthStore = vi.fn();
vi.mock('@/store/authStore', () => ({
  useAuthStore: () => mockAuthStore(),
}));

const mockUIStore = vi.fn();
vi.mock('@/store/uiStore', () => ({
  useUIStore: (selector: unknown) => {
    const store = mockUIStore();
    return typeof selector === 'function' ? selector(store) : store;
  },
}));

const mockUseAdminGuard = vi.fn();
vi.mock('@/admin/hooks/useAdminGuard', () => ({
  useAdminGuard: () => mockUseAdminGuard(),
}));

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function setupLoggedOut() {
  mockAuthStore.mockReturnValue({
    token: null,
    user: null,
    logout: vi.fn(),
  });
  mockUIStore.mockReturnValue({
    addToast: vi.fn(),
  });
  mockUseAdminGuard.mockReturnValue({ isAdmin: false });
}

function setupLoggedIn(overrides: { isAdmin?: boolean } = {}) {
  mockAuthStore.mockReturnValue({
    token: 'test-token',
    user: { id: '1', username: 'testuser', nickname: 'Test User', avatar: null },
    logout: vi.fn(),
  });
  mockUIStore.mockReturnValue({
    addToast: vi.fn(),
  });
  mockUseAdminGuard.mockReturnValue({ isAdmin: overrides.isAdmin ?? false });
}

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders brand name', () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    expect(screen.getByText('EasyOrange')).toBeInTheDocument();
  });

  it('renders navigation links', () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    expect(screen.getByText('首页')).toBeInTheDocument();
    expect(screen.getByText('商品')).toBeInTheDocument();
  });

  it('shows login button when logged out', () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    expect(screen.getByTestId('btn-login')).toBeInTheDocument();
    expect(screen.getByText('登录')).toBeInTheDocument();
  });

  it('shows user menu and hides login button when logged in', () => {
    setupLoggedIn();
    renderWithProviders(<Header />);
    expect(screen.queryByTestId('btn-login')).not.toBeInTheDocument();
    expect(screen.getByText('Test User')).toBeInTheDocument();
  });

  it('shows search button', () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    expect(screen.getByLabelText('搜索')).toBeInTheDocument();
  });

  it('navigates to login on login button click', async () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    await userEvent.click(screen.getByTestId('btn-login'));
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  it('shows publish link', () => {
    setupLoggedOut();
    renderWithProviders(<Header />);
    expect(screen.getByText('发布')).toBeInTheDocument();
  });

  it('opens user menu dropdown on avatar click', async () => {
    setupLoggedIn();
    renderWithProviders(<Header />);

    await userEvent.click(screen.getByTestId('btn-user-menu'));
    expect(screen.getByText('个人中心')).toBeInTheDocument();
    expect(screen.getByText('我的收藏')).toBeInTheDocument();
    expect(screen.getByText('我的订单')).toBeInTheDocument();
  });

  it('shows admin link when user is admin', async () => {
    setupLoggedIn({ isAdmin: true });
    renderWithProviders(<Header />);

    await userEvent.click(screen.getByTestId('btn-user-menu'));
    expect(screen.getByText('后台管理')).toBeInTheDocument();
  });

  it('does not show admin link when user is not admin', async () => {
    setupLoggedIn({ isAdmin: false });
    renderWithProviders(<Header />);

    await userEvent.click(screen.getByTestId('btn-user-menu'));
    expect(screen.queryByText('后台管理')).not.toBeInTheDocument();
  });

  it('has logout button in user menu', async () => {
    const logout = vi.fn();
    mockAuthStore.mockReturnValue({
      token: 'test-token',
      user: { id: '1', username: 'testuser', nickname: 'Test User', avatar: null },
      logout,
    });
    mockUIStore.mockReturnValue({ addToast: vi.fn() });
    mockUseAdminGuard.mockReturnValue({ isAdmin: false });

    renderWithProviders(<Header />);

    await userEvent.click(screen.getByTestId('btn-user-menu'));
    await userEvent.click(screen.getByTestId('btn-logout'));
    expect(logout).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
