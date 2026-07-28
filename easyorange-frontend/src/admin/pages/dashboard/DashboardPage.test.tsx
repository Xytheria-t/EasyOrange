import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import DashboardPage from './DashboardPage';

// ─── Hook mocks ───
const mockUseDashboardStats = vi.fn();
const mockUsePendingItems = vi.fn();
const mockUseRecentUsers = vi.fn();
const mockUseRecentProducts = vi.fn();
const mockUseTrend = vi.fn();
const mockUseUserActivityHeatmap = vi.fn();
const mockUseTopProducts = vi.fn();

vi.mock('../../hooks/useAdminDashboard', () => ({
    useDashboardStats: (...args: unknown[]) => mockUseDashboardStats(...args),
    usePendingItems: (...args: unknown[]) => mockUsePendingItems(...args),
    useRecentUsers: (...args: unknown[]) => mockUseRecentUsers(...args),
    useRecentProducts: (...args: unknown[]) => mockUseRecentProducts(...args),
    useTrend: (...args: unknown[]) => mockUseTrend(...args),
    useUserActivityHeatmap: (...args: unknown[]) => mockUseUserActivityHeatmap(...args),
    useTopProducts: (...args: unknown[]) => mockUseTopProducts(...args),
}));

vi.mock('./StatCard', () => ({
    StatCard: ({ title, value }: { title: string; value: number | string }) => (
        <div data-testid={`stat-card-${title}`}>
            {title}: {value}
        </div>
    ),
}));

vi.mock('./charts/ActivityHeatmap', () => ({ default: () => <div data-testid="activity-heatmap" /> }));
vi.mock('./charts/lazyCharts', () => ({
    LazyTrendChart: () => <div data-testid="trend-chart" />,
    LazyTopProductsChart: () => <div data-testid="top-products-chart" />,
}));

vi.mock('react-router-dom', async importOriginal => {
    const actual = await importOriginal<typeof import('react-router-dom')>();
    return {
        ...actual,
        Link: ({ children, to, ...props }: { children: React.ReactNode; to: string; [key: string]: unknown }) => (
            <a href={to} {...props}>
                {children}
            </a>
        ),
    };
});

// ─── Default mock data ───
function setupDefaultMocks() {
    mockUseDashboardStats.mockReturnValue({
        data: {
            totalUsers: 1000,
            todayNewUsers: 25,
            totalProducts: 500,
            pendingProducts: 10,
            totalOrders: 300,
            todayOrders: 15,
            totalRevenue: 50000,
            pendingReports: 3,
        },
        isLoading: false,
        isError: false,
        error: null,
    });

    mockUsePendingItems.mockReturnValue({
        data: {
            pendingProducts: 10,
            pendingReports: 3,
            pendingOrders: 5,
            recentReports: [],
        },
        isLoading: false,
        isError: false,
        error: null,
    });

    mockUseRecentUsers.mockReturnValue({
        data: [
            {
                userId: '1',
                username: 'alice',
                nickname: 'Alice',
                avatar: null,
                email: null,
                phone: null,
                userType: null,
                userTypeDesc: null,
                status: null,
                statusDesc: null,
                createTime: new Date(Date.now() - 300_000).toISOString(), // 5 minutes ago
            },
            {
                userId: '2',
                username: 'bob',
                nickname: 'Bob',
                avatar: null,
                email: null,
                phone: null,
                userType: null,
                userTypeDesc: null,
                status: null,
                statusDesc: null,
                createTime: new Date(Date.now() - 600_000).toISOString(), // 10 minutes ago
            },
        ],
        isLoading: false,
        isError: false,
        error: null,
    });

    mockUseRecentProducts.mockReturnValue({
        data: [
            {
                productId: '1',
                name: '高等数学教材',
                price: 45,
                mainImage: null,
                status: 1,
                statusDesc: '上架',
                sellerId: null,
                sellerName: null,
                categoryName: null,
                viewCount: null,
                createTime: new Date(Date.now() - 120_000).toISOString(), // 2 minutes ago
            },
        ],
        isLoading: false,
        isError: false,
        error: null,
    });

    mockUseTrend.mockReturnValue({ data: [] });
    mockUseUserActivityHeatmap.mockReturnValue({ data: [] });
    mockUseTopProducts.mockReturnValue({ data: [] });
}

describe('DashboardPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();
        // Set a deterministic time for greeting and relative-time assertions
        vi.setSystemTime(new Date(2026, 4, 16, 10, 0, 0));
        setupDefaultMocks();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    // ── Test 1: Greeting ──
    it('renders greeting title with current time period', () => {
        renderWithProviders(<DashboardPage />);
        // 10:00 AM → "上午好" (rendered inside a span with "，", use partial match)
        expect(screen.getByText(/上午好/)).toBeInTheDocument();
        expect(screen.getByText('管理员')).toBeInTheDocument();
    });

    // ── Test 2: Stat cards loading (skeleton) ──
    it('renders skeleton placeholders when stats are loading', () => {
        mockUseDashboardStats.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            error: null,
        });

        renderWithProviders(<DashboardPage />);

        // No StatCard components should render
        expect(screen.queryByTestId('stat-card-用户总数')).not.toBeInTheDocument();
        expect(screen.queryByTestId('stat-card-今日新增')).not.toBeInTheDocument();
        expect(screen.queryByTestId('stat-card-商品总数')).not.toBeInTheDocument();
        expect(screen.queryByTestId('stat-card-待审商品')).not.toBeInTheDocument();
        expect(screen.queryByTestId('stat-card-订单总量')).not.toBeInTheDocument();
    });

    // ── Test 3: Stat cards with data ──
    it('renders all 5 stat cards with data', () => {
        renderWithProviders(<DashboardPage />);

        expect(screen.getByTestId('stat-card-用户总数')).toBeInTheDocument();
        expect(screen.getByTestId('stat-card-今日新增')).toBeInTheDocument();
        expect(screen.getByTestId('stat-card-商品总数')).toBeInTheDocument();
        expect(screen.getByTestId('stat-card-待审商品')).toBeInTheDocument();
        expect(screen.getByTestId('stat-card-订单总量')).toBeInTheDocument();

        // Verify values are passed through
        expect(screen.getByTestId('stat-card-用户总数')).toHaveTextContent('1000');
        expect(screen.getByTestId('stat-card-今日新增')).toHaveTextContent('25');
        expect(screen.getByTestId('stat-card-订单总量')).toHaveTextContent('300');
    });

    // ── Test 4: Quick actions panel ──
    it('renders quick actions panel with all 6 action items', () => {
        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('快捷操作')).toBeInTheDocument();

        // All six action labels
        expect(screen.getByText('商品审核')).toBeInTheDocument();
        expect(screen.getByText('处理举报')).toBeInTheDocument();
        expect(screen.getByText('订单管理')).toBeInTheDocument();
        expect(screen.getByText('用户管理')).toBeInTheDocument();
        expect(screen.getByText('分类管理')).toBeInTheDocument();
        expect(screen.getByText('数据统计')).toBeInTheDocument();

        // Each action links to the correct route
        expect(screen.getByText('商品审核').closest('a')).toHaveAttribute('href', '/admin/products?status=0');
        expect(screen.getByText('处理举报').closest('a')).toHaveAttribute('href', '/admin/reports');
        expect(screen.getByText('订单管理').closest('a')).toHaveAttribute('href', '/admin/orders');
        expect(screen.getByText('用户管理').closest('a')).toHaveAttribute('href', '/admin/users');
        expect(screen.getByText('分类管理').closest('a')).toHaveAttribute('href', '/admin/categories');
        expect(screen.getByText('数据统计').closest('a')).toHaveAttribute('href', '/admin/stats');
    });

    // ── Test 5: Pending items alert bar ──
    it('renders pending items alert bar with counts when items exist', () => {
        renderWithProviders(<DashboardPage />);

        // Section header
        expect(screen.getByText('待处理事项')).toBeInTheDocument();

        // Total pending badge (10 + 3 + 5 = 18)
        expect(screen.getByText('18')).toBeInTheDocument();

        // Each pending item section
        expect(screen.getByText(content => content.includes('商品待审核'))).toBeInTheDocument();
        expect(screen.getByText(content => content.includes('举报待处理'))).toBeInTheDocument();
        expect(screen.getByText(content => content.includes('待处理订单'))).toBeInTheDocument();

        // "立即处理" links exist (one per pending type = 3)
        expect(screen.getAllByText('立即处理')).toHaveLength(3);
    });

    // ── Test 6: Recent users section ──
    it('renders "最近注册用户" section with user avatars and names', () => {
        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('最近注册用户')).toBeInTheDocument();
        expect(screen.getByText('Alice')).toBeInTheDocument();
        expect(screen.getByText('@alice')).toBeInTheDocument();
        expect(screen.getByText('Bob')).toBeInTheDocument();
        expect(screen.getByText('@bob')).toBeInTheDocument();

        // Relative time (5 and 10 minutes ago with fake time at 10:00)
        expect(screen.getByText('5分钟前')).toBeInTheDocument();
        expect(screen.getByText('10分钟前')).toBeInTheDocument();

        // Avatar initials
        expect(screen.getByText('A')).toBeInTheDocument();
        expect(screen.getByText('B')).toBeInTheDocument();

        // "查看全部" links (one for users, one for products)
        const viewAllLinks = screen.getAllByText('查看全部');
        const usersViewAll = viewAllLinks.find(link => link.closest('a')?.getAttribute('href') === '/admin/users');
        expect(usersViewAll).toBeTruthy();
    });

    // ── Test 7: Recent products section ──
    it('renders "最近上架商品" section with product info', () => {
        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('最近上架商品')).toBeInTheDocument();
        expect(screen.getByText('高等数学教材')).toBeInTheDocument();
        expect(screen.getByText('¥45')).toBeInTheDocument();

        // Relative time
        expect(screen.getByText('2分钟前')).toBeInTheDocument();

        // "查看全部" links (one for users, one for products)
        const productViewAllLinks = screen.getAllByText('查看全部');
        const productViewAll = productViewAllLinks.find(
            link => link.closest('a')?.getAttribute('href') === '/admin/products'
        );
        expect(productViewAll).toBeTruthy();

        // Product icon (contains "教材" → gets the book emoji)
        expect(screen.getByText('\u{1F4DA}')).toBeInTheDocument();
    });

    // ── Test 8: Error banner ──
    it('shows error banner with retry button when statsError is true', () => {
        mockUseDashboardStats.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
            error: new Error('Network failure'),
        });

        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('数据加载失败')).toBeInTheDocument();
        expect(screen.getByText('刷新页面')).toBeInTheDocument();

        // Error detail message is shown
        expect(screen.getByText(/Network failure/)).toBeInTheDocument();

        // Stat cards fall back to 0 when data is undefined
        expect(screen.getByTestId('stat-card-用户总数')).toHaveTextContent('0');
    });

    // ── Test 9: Empty state for recent users ──
    it('shows empty state for recent users when list is empty', () => {
        mockUseRecentUsers.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
        });

        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('暂无用户记录')).toBeInTheDocument();
        expect(screen.getByText('当前暂无新注册用户')).toBeInTheDocument();

        // User names should NOT render
        expect(screen.queryByText('Alice')).not.toBeInTheDocument();
        expect(screen.queryByText('@alice')).not.toBeInTheDocument();
    });

    // ── Test 10: Empty state for recent products ──
    it('shows empty state for recent products when list is empty', () => {
        mockUseRecentProducts.mockReturnValue({
            data: [],
            isLoading: false,
            isError: false,
            error: null,
        });

        renderWithProviders(<DashboardPage />);

        expect(screen.getByText('暂无商品记录')).toBeInTheDocument();
        expect(screen.getByText('当前暂无新上架商品')).toBeInTheDocument();

        // Product name should NOT render
        expect(screen.queryByText('高等数学教材')).not.toBeInTheDocument();
    });

    // ── Test 11: No pending alert when all zeros ──
    it('does not render the pending items alert bar when all counts are zero', () => {
        mockUsePendingItems.mockReturnValue({
            data: {
                pendingProducts: 0,
                pendingReports: 0,
                pendingOrders: 0,
                recentReports: [],
            },
            isLoading: false,
            isError: false,
            error: null,
        });

        renderWithProviders(<DashboardPage />);

        expect(screen.queryByText('待处理事项')).not.toBeInTheDocument();
    });

    // ── Test 12: Layout elements ──
    it('renders admin dashboard layout elements including subtitle and charts', () => {
        renderWithProviders(<DashboardPage />);

        // Subtitle
        expect(screen.getByText('平台运营数据一览，掌控全局动态')).toBeInTheDocument();

        // Live clock pill renders
        const clockPill = screen.getByText(/5月16日/);
        expect(clockPill).toBeInTheDocument();

        // Chart section headers
        expect(screen.getByText('趋势概览')).toBeInTheDocument();
        expect(screen.getByText('用户活跃时段')).toBeInTheDocument();
        expect(screen.getByText('Top 浏览量商品')).toBeInTheDocument();

        // Mocked chart components render
        expect(screen.getByTestId('trend-chart')).toBeInTheDocument();
        expect(screen.getByTestId('activity-heatmap')).toBeInTheDocument();
        expect(screen.getByTestId('top-products-chart')).toBeInTheDocument();
    });

    // ── Bonus: loading skeletons for users and products sections ──
    it('shows loading skeletons for recent users and products sections', () => {
        mockUseRecentUsers.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            error: null,
        });
        mockUseRecentProducts.mockReturnValue({
            data: undefined,
            isLoading: true,
            isError: false,
            error: null,
        });

        renderWithProviders(<DashboardPage />);

        // Section headers still render
        expect(screen.getByText('最近注册用户')).toBeInTheDocument();
        expect(screen.getByText('最近上架商品')).toBeInTheDocument();

        // Data content should not render
        expect(screen.queryByText('Alice')).not.toBeInTheDocument();
        expect(screen.queryByText('高等数学教材')).not.toBeInTheDocument();
    });

    // ── Bonus: greet at different time periods ──
    it('shows different greetings based on time of day', () => {
        // Afternoon
        vi.setSystemTime(new Date(2026, 4, 16, 14, 30, 0));
        const { unmount } = renderWithProviders(<DashboardPage />);
        expect(screen.getByText(/下午好/)).toBeInTheDocument();
        unmount();

        // Evening
        vi.setSystemTime(new Date(2026, 4, 16, 19, 0, 0));
        renderWithProviders(<DashboardPage />);
        expect(screen.getByText(/晚上好/)).toBeInTheDocument();
    });
});
