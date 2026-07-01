import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
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

const mockUseLogout = vi.fn();
vi.mock('@/hooks', () => ({
    useLogout: () => mockUseLogout,
}));

function renderWithProviders(ui: React.ReactElement) {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={qc}>
            <MemoryRouter>{ui}</MemoryRouter>
        </QueryClientProvider>
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

    it('shows login button when logged out', () => {
        setupLoggedOut();
        renderWithProviders(<Header />);

        expect(screen.getByText('登录')).toBeInTheDocument();
    });

    it('shows user nickname when logged in', async () => {
        setupLoggedIn();
        renderWithProviders(<Header />);

        const userButton = screen.getByTestId('btn-user-menu');
        expect(userButton).toBeInTheDocument();

        await userEvent.click(userButton);
        expect(screen.getByText('Test User')).toBeInTheDocument();
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
        mockAuthStore.mockReturnValue({
            token: 'test-token',
            user: { id: '1', username: 'testuser', nickname: 'Test User', avatar: null },
        });
        mockUIStore.mockReturnValue({ addToast: vi.fn() });
        mockUseAdminGuard.mockReturnValue({ isAdmin: false });

        renderWithProviders(<Header />);

        await userEvent.click(screen.getByTestId('btn-user-menu'));
        await userEvent.click(screen.getByTestId('btn-logout'));
        expect(mockUseLogout).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });
});
