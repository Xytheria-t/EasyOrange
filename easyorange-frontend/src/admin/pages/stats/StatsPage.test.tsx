import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { ActivityItem, CategoryResponse, DashboardStats, OrderStatsResponse, TrendItem } from '../../types/admin';
import StatsPage from './StatsPage';

// ─── Hook mocks ───
const mockUseDashboardStats = vi.fn();
const mockUseAdminCategories = vi.fn();
const mockUseAdminOrderStats = vi.fn();
const mockUseTrend = vi.fn();
const mockUseRecentActivity = vi.fn();

vi.mock('../../hooks', () => ({
    useDashboardStats: (...args: unknown[]) => mockUseDashboardStats(...args),
    useAdminCategories: (...args: unknown[]) => mockUseAdminCategories(...args),
    useAdminOrderStats: (...args: unknown[]) => mockUseAdminOrderStats(...args),
    useTrend: (...args: unknown[]) => mockUseTrend(...args),
    useRecentActivity: (...args: unknown[]) => mockUseRecentActivity(...args),
}));

vi.mock('./charts/lazyCharts', () => ({
    LazyTrendChart: ({ data: _data, isCompact, height }: { data: TrendItem[]; isCompact: boolean; height: number }) => (
        <div data-testid="trend-chart" data-compact={isCompact} data-height={height}>
            TrendChart
        </div>
    ),
}));
// ─── Sample data ───
const sampleStats: DashboardStats = {
    totalUsers: 1000,
    todayNewUsers: 25,
    totalProducts: 500,
    pendingProducts: 10,
    totalOrders: 300,
    todayOrders: 15,
    totalRevenue: 50000,
};

const sampleOrderStats: OrderStatsResponse = {
    totalOrders: 300,
    todayOrders: 15,
    pendingPayment: 5,
    toShip: 10,
    toReceive: 8,
    completed: 250,
    cancelled: 20,
    refunded: 7,
    totalRevenue: 50000,
    todayRevenue: 3000,
};

const sampleCategories: CategoryResponse[] = [
    {
        categoryId: '1',
        name: '电子产品',
        parentId: null,
        parentName: null,
        level: 1,
        sortOrder: 1,
        status: 1,
        productCount: 100,
        createTime: null,
        updateTime: null,
    },
    {
        categoryId: '2',
        name: '图书',
        parentId: null,
        parentName: null,
        level: 1,
        sortOrder: 2,
        status: 1,
        productCount: 50,
        createTime: null,
        updateTime: null,
    },
    {
        categoryId: '3',
        name: '服装',
        parentId: null,
        parentName: null,
        level: 1,
        sortOrder: 3,
        status: 0,
        productCount: 30,
        createTime: null,
        updateTime: null,
    },
    // 二级分类：一级计数已归并它，进分布图会把同一件商品算两遍
    {
        categoryId: '4',
        name: '手机',
        parentId: '1',
        parentName: '电子产品',
        level: 2,
        sortOrder: 1,
        status: 1,
        productCount: 60,
        createTime: null,
        updateTime: null,
    },
];

const sampleTrend: TrendItem[] = [
    { month: '2026-01', users: 100, products: 50, orders: 30 },
    { month: '2026-02', users: 120, products: 60, orders: 40 },
];

const sampleActivity: ActivityItem[] = [
    { time: '10分钟前', text: '新用户注册', type: 'user' },
    { time: '20分钟前', text: '新订单创建', type: 'order' },
];

function setupMocks(
    overrides: Partial<{
        stats: DashboardStats | undefined;
        statsLoading: boolean;
        categories: CategoryResponse[] | undefined;
        categoriesLoading: boolean;
        orderStats: OrderStatsResponse | undefined;
        orderStatsLoading: boolean;
        orderStatsError: boolean;
        trend: TrendItem[];
        trendError: boolean;
        activity: ActivityItem[];
        activityError: boolean;
    }> = {}
) {
    const {
        stats = sampleStats,
        statsLoading = false,
        categories = sampleCategories,
        categoriesLoading = false,
        // 显式传 undefined 表示「后端没返回」，不能被默认值顶掉
        orderStats = 'orderStats' in overrides ? overrides.orderStats : sampleOrderStats,
        orderStatsLoading = false,
        orderStatsError = false,
        trend = sampleTrend,
        trendError = false,
        activity = sampleActivity,
        activityError = false,
    } = overrides;

    mockUseDashboardStats.mockReturnValue({ data: stats, isLoading: statsLoading });
    mockUseAdminCategories.mockReturnValue({
        data: categories,
        isLoading: categoriesLoading,
        isError: false,
    });
    mockUseAdminOrderStats.mockReturnValue({
        data: orderStats,
        isLoading: orderStatsLoading,
        isError: orderStatsError,
    });
    mockUseTrend.mockReturnValue({ data: trend, isLoading: false, isError: trendError });
    mockUseRecentActivity.mockReturnValue({ data: activity, isLoading: false, isError: activityError });
}

describe('StatsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupMocks();
    });

    // ── Test 1: Page title ──
    it('renders page title "数据统计"', () => {
        renderWithProviders(<StatsPage />);
        expect(screen.getByText('数据统计')).toBeInTheDocument();
        expect(screen.getByText(/平台运营数据概览与趋势分析/)).toBeInTheDocument();
    });

    // ── Test 2: Shows stat cards with data ──
    it('renders stat cards with values', () => {
        renderWithProviders(<StatsPage />);

        expect(screen.getByText('总用户数')).toBeInTheDocument();
        expect(screen.getByText('总商品数')).toBeInTheDocument();
        expect(screen.getByText('总订单数')).toBeInTheDocument();
        expect(screen.getByText('今日新增用户')).toBeInTheDocument();

        // Values
        expect(screen.getByText('1,000')).toBeInTheDocument();
        expect(screen.getByText('500')).toBeInTheDocument();
        expect(screen.getByText('300')).toBeInTheDocument();
        expect(screen.getByText('25')).toBeInTheDocument();
    });

    // ── Test 3: Loading stat cards shows placeholder ──
    it('shows dash placeholder when stats loading', () => {
        setupMocks({ stats: undefined, statsLoading: true });
        renderWithProviders(<StatsPage />);

        // Should show — for values
        const dashes = screen.getAllByText('—');
        expect(dashes.length).toBeGreaterThan(0);
    });

    // ── Test 4: Order stats section ──
    it('renders order stats summary', () => {
        renderWithProviders(<StatsPage />);

        expect(screen.getByText(/今日订单/)).toBeInTheDocument();
        expect(screen.getByText(/待发货/)).toBeInTheDocument();
        expect(screen.getByText(/待收货/)).toBeInTheDocument();
        expect(screen.getByText(/已完成/)).toBeInTheDocument();
        expect(screen.getByText(/今日营收/)).toBeInTheDocument();

        // 值带单位，避免"15"到底是笔数还是件数说不清
        expect(screen.getByText('15 笔')).toBeInTheDocument();
        expect(screen.getByText('10 笔')).toBeInTheDocument();
        expect(screen.getByText('8 笔')).toBeInTheDocument();
        expect(screen.getByText('250 笔')).toBeInTheDocument();
    });

    // ── Test 5: Trend chart section ──
    it('renders trend chart section', () => {
        renderWithProviders(<StatsPage />);

        expect(screen.getByText('月度趋势')).toBeInTheDocument();
        expect(screen.getByTestId('trend-chart')).toBeInTheDocument();
    });

    // ── Test 6: Category distribution ──
    it('renders category distribution with bars', () => {
        renderWithProviders(<StatsPage />);

        expect(screen.getByText(/商品分类分布/)).toBeInTheDocument();
        expect(screen.getByText('电子产品')).toBeInTheDocument();
        // 二级分类已被一级归并，不得再进分布图（否则同一件商品计两次）
        expect(screen.queryByText('手机')).not.toBeInTheDocument();
        expect(screen.getByText('图书')).toBeInTheDocument();
        expect(screen.getByText('服装')).toBeInTheDocument();

        // Count percentages
        expect(screen.getByText(/100 件/)).toBeInTheDocument();
        expect(screen.getByText(/50 件/)).toBeInTheDocument();
        expect(screen.getByText(/30 件/)).toBeInTheDocument();
    });

    // ── Test 7: Category distribution loading ──
    it('shows loading state for category distribution', () => {
        setupMocks({ categories: undefined, categoriesLoading: true });
        renderWithProviders(<StatsPage />);

        expect(screen.getByText('加载中…')).toBeInTheDocument();
    });

    // ── Test 8: Category distribution empty ──
    it('shows empty state for category distribution', () => {
        setupMocks({ categories: [] });
        renderWithProviders(<StatsPage />);

        expect(screen.getAllByText('暂无数据').length).toBeGreaterThan(0);
    });

    // ── Test 9: Recent activity section ──
    it('renders recent activity list', () => {
        renderWithProviders(<StatsPage />);

        expect(screen.getByText(/最近动态/)).toBeInTheDocument();
        expect(screen.getByText('新用户注册')).toBeInTheDocument();
        expect(screen.getByText('新订单创建')).toBeInTheDocument();
        expect(screen.getByText('10分钟前')).toBeInTheDocument();
        expect(screen.getByText('20分钟前')).toBeInTheDocument();
    });

    // ── Test 10: Recent activity empty ──
    it('shows empty state for recent activity', () => {
        setupMocks({ activity: [] });
        renderWithProviders(<StatsPage />);

        expect(screen.getAllByText('暂无数据').length).toBeGreaterThan(0);
    });

    // ── Test 11: Order stats null/undefined ──
    it('handles missing order stats', () => {
        setupMocks({ orderStats: undefined });
        renderWithProviders(<StatsPage />);

        expect(screen.queryByText('今日订单')).not.toBeInTheDocument();
        expect(screen.queryByText('待发货')).not.toBeInTheDocument();
    });

    // ── Test 12: 失败不能伪装成空数据 ──
    it('shows a failure state instead of empty data for order summary and activity', () => {
        setupMocks({ orderStatsError: true, activityError: true, trendError: true });
        renderWithProviders(<StatsPage />);

        expect(screen.getByText(/订单数据加载失败/)).toBeInTheDocument();
        expect(screen.getAllByText('加载失败，请刷新重试').length).toBeGreaterThan(0);
        // 失败时不能同时声称「暂无数据」
        expect(screen.queryByText('暂无数据')).not.toBeInTheDocument();
    });
});
