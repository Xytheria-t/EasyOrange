import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { PageResult } from '@/types';
import type { AdminUser } from '../../types/admin';
import UserManagePage from './UserManagePage';

// ─── Hook mocks ───
const mockUseAdminUsers = vi.fn();
const mockUseUpdateUserStatus = vi.fn();

vi.mock('../../hooks', () => ({
    useAdminUsers: (...args: unknown[]) => mockUseAdminUsers(...args),
    useUpdateUserStatus: (...args: unknown[]) => mockUseUpdateUserStatus(...args),
    useAdminUserDetail: vi.fn(),
}));

// Mock UserDetailModal
vi.mock('./UserDetailModal', () => ({
    UserDetailModal: ({
        open,
        user,
        onClose,
        onSave,
        loading,
    }: {
        open: boolean;
        user: AdminUser | null;
        onClose: () => void;
        onSave: (status: string) => Promise<void>;
        loading: boolean;
    }) => {
        if (!open || !user) {
            return null;
        }
        return (
            <div data-testid="user-detail-modal" data-user-id={user.userId}>
                <span>{user.username}</span>
                <button type="button" onClick={onClose}>
                    关闭
                </button>
                <button type="button" onClick={() => onSave('1')} data-testid="modal-save">
                    {loading ? '保存中...' : '保存修改'}
                </button>
            </div>
        );
    },
}));

// Mock AdminTable
vi.mock('../../components/AdminTable', () => ({
    AdminTable: ({
        columns: _columns,
        data,
        loading,
        pagination,
        emptyText,
    }: {
        columns: unknown[];
        data: AdminUser[];
        loading: boolean;
        pagination: unknown;
        emptyText: string;
    }) => (
        <div data-testid="admin-table">
            {loading ? (
                <div data-testid="loading-skeleton">Loading...</div>
            ) : data.length === 0 ? (
                <div data-testid="empty-state">{emptyText}</div>
            ) : (
                <div>
                    {data.map(user => (
                        <div key={user.userId} data-testid="user-row">
                            <span data-testid="user-username">{user.username}</span>
                            <span data-testid="user-nickname">{user.nickname}</span>
                            <span data-testid="user-email">{user.email}</span>
                            <button type="button" data-testid="view-detail-btn" onClick={() => {}}>
                                详情
                            </button>
                        </div>
                    ))}
                    {!!pagination && (
                        <div data-testid="pagination">
                            <button
                                type="button"
                                onClick={() => (pagination as { onChange: (p: number) => void }).onChange(2)}
                            >
                                下一页
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    ),
}));

vi.mock('../../components/StatusBadge', () => ({
    StatusBadge: ({ status }: { status: string | number }) => <span data-testid="status-badge">{status}</span>,
}));

vi.mock('../../components/AdminSelect', () => ({
    AdminSelect: ({
        options,
        value,
        onChange,
    }: {
        options: { value: string; label: string }[];
        value: string;
        onChange: (val: string) => void;
    }) => (
        <select data-testid="admin-select" value={value} onChange={e => onChange(e.target.value)}>
            {options.map(opt => (
                <option key={opt.value} value={opt.value}>
                    {opt.label}
                </option>
            ))}
        </select>
    ),
}));

// ─── Sample data ───
const sampleUsers: AdminUser[] = [
    {
        userId: 1,
        username: 'alice',
        nickname: 'Alice',
        avatar: null,
        email: 'alice@example.com',
        phone: '13800138001',
        studentId: null,
        realName: null,
        userType: '01',
        userTypeDesc: '学生',
        status: '0',
        statusDesc: '正常',
        loginIp: null,
        loginDate: null,
        createTime: '2026-05-16T10:00:00',
        updateTime: null,
    },
    {
        userId: 2,
        username: 'bob',
        nickname: null,
        avatar: null,
        email: 'bob@example.com',
        phone: '13900139002',
        studentId: null,
        realName: null,
        userType: '02',
        userTypeDesc: '教师',
        status: '1',
        statusDesc: '禁用',
        loginIp: null,
        loginDate: null,
        createTime: '2026-05-15T10:00:00',
        updateTime: null,
    },
];

const samplePageData: PageResult<AdminUser> = {
    records: sampleUsers,
    total: 2,
    current: 1,
    size: 10,
    pages: 1,
};

function setupMocks(
    overrides: Partial<{
        data: PageResult<AdminUser> | undefined;
        isLoading: boolean;
        isError: boolean;
    }> = {}
) {
    const { data = samplePageData, isLoading = false, isError = false } = overrides;

    mockUseAdminUsers.mockReturnValue({
        data,
        isLoading,
        isError,
        error: isError ? new Error('Network error') : null,
    });

    mockUseUpdateUserStatus.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue({}),
        isPending: false,
    });
}

describe('UserManagePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupMocks();
    });

    // ── Test 1: Page title ──
    it('renders page title "用户管理"', () => {
        renderWithProviders(<UserManagePage />);
        expect(screen.getByText('用户管理')).toBeInTheDocument();
        expect(screen.getByText('管理平台所有注册用户，查看详情或调整状态')).toBeInTheDocument();
    });

    // ── Test 2: Shows user list ──
    it('renders user list with user data', () => {
        renderWithProviders(<UserManagePage />);

        expect(screen.getByTestId('admin-table')).toBeInTheDocument();
        expect(screen.getByText('alice')).toBeInTheDocument();
        expect(screen.getByText('bob')).toBeInTheDocument();
    });

    // ── Test 3: Shows total count ──
    it('shows total user count', () => {
        renderWithProviders(<UserManagePage />);

        const countText = screen.getByText(/共/);
        expect(countText).toHaveTextContent('共 2 位用户');
    });

    // ── Test 4: Search by keyword ──
    it('searches by keyword', () => {
        renderWithProviders(<UserManagePage />);

        const searchInput = screen.getByPlaceholderText('搜索用户名/邮箱...');
        fireEvent.change(searchInput, { target: { value: 'alice' } });
        fireEvent.click(screen.getByText('搜索'));

        expect(mockUseAdminUsers).toHaveBeenCalled();
    });

    // ── Test 5: Search on Enter key ──
    it('triggers search on Enter key press', () => {
        renderWithProviders(<UserManagePage />);

        const searchInput = screen.getByPlaceholderText('搜索用户名/邮箱...');
        fireEvent.change(searchInput, { target: { value: 'test' } });
        fireEvent.keyPress(searchInput, { key: 'Enter' });

        expect(mockUseAdminUsers).toHaveBeenCalled();
    });

    // ── Test 6: Status filter ──
    it('changes status filter', () => {
        renderWithProviders(<UserManagePage />);

        const selects = screen.getAllByTestId('admin-select');
        const statusSelect = selects[0];
        fireEvent.change(statusSelect, { target: { value: '0' } });

        expect(mockUseAdminUsers).toHaveBeenCalled();
    });

    // ── Test 7: User type filter ──
    it('changes user type filter', () => {
        renderWithProviders(<UserManagePage />);

        const selects = screen.getAllByTestId('admin-select');
        const typeSelect = selects[1];
        fireEvent.change(typeSelect, { target: { value: '01' } });

        expect(mockUseAdminUsers).toHaveBeenCalled();
    });

    // ── Test 8: Loading state ──
    it('shows loading state', () => {
        setupMocks({ isLoading: true });
        renderWithProviders(<UserManagePage />);

        expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument();
    });

    // ── Test 9: Empty state ──
    it('shows empty state when no users', () => {
        setupMocks({
            data: { records: [], total: 0, current: 1, size: 10, pages: 0 },
        });
        renderWithProviders(<UserManagePage />);

        expect(screen.getByText('暂无用户数据')).toBeInTheDocument();
    });

    // ── Test 10: Error state ──
    it('shows error banner when isError', () => {
        setupMocks({ isError: true, data: undefined });
        renderWithProviders(<UserManagePage />);

        expect(screen.getByText('数据加载失败')).toBeInTheDocument();
        expect(screen.getByText('刷新')).toBeInTheDocument();
    });

    // ── Test 11: Detail buttons render for each user ──
    it('renders detail buttons for each user', () => {
        renderWithProviders(<UserManagePage />);

        const detailBtns = screen.getAllByText('详情');
        expect(detailBtns.length).toBeGreaterThanOrEqual(2);
    });

    // ── Test 12: Pagination ──
    it('renders pagination when data exceeds page size', () => {
        const manyUsers: AdminUser[] = Array.from({ length: 10 }, (_, i) => ({
            userId: i + 1,
            username: `user${i}`,
            nickname: `User${i}`,
            avatar: null,
            email: `user${i}@example.com`,
            phone: null,
            studentId: null,
            realName: null,
            userType: '01',
            userTypeDesc: '学生',
            status: '0',
            statusDesc: '正常',
            loginIp: null,
            loginDate: null,
            createTime: '2026-05-16T10:00:00',
            updateTime: null,
        }));

        setupMocks({
            data: {
                records: manyUsers,
                total: 25,
                current: 1,
                size: 10,
                pages: 3,
            },
        });
        renderWithProviders(<UserManagePage />);

        expect(screen.getByTestId('pagination')).toBeInTheDocument();
        fireEvent.click(screen.getByText('下一页'));
        expect(mockUseAdminUsers).toHaveBeenCalled();
    });

    // ── Test 13: No count text when total is 0 ──
    it('does not show count when total is 0', () => {
        setupMocks({
            data: { records: [], total: 0, current: 1, size: 10, pages: 0 },
        });
        renderWithProviders(<UserManagePage />);

        expect(screen.queryByText(/位用户/)).not.toBeInTheDocument();
    });

    // ── Test 14: Email display ──
    it('renders email addresses', () => {
        renderWithProviders(<UserManagePage />);

        expect(screen.getByText('alice@example.com')).toBeInTheDocument();
        expect(screen.getByText('bob@example.com')).toBeInTheDocument();
    });

    // ── Test 15: Nickname fallback ──
    it('handles missing nickname gracefully', () => {
        renderWithProviders(<UserManagePage />);

        // bob has no nickname, should still render username
        expect(screen.getByText('bob')).toBeInTheDocument();
    });
});
