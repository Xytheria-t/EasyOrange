import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import StatsPage from './StatsPage';
import type { DashboardStats, OrderStatsResponse, CategoryResponse, TrendItem, ActivityItem } from '../../types/admin';

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

// Mock TrendChart
vi.mock('../dashboard/charts/TrendChart', () => ({
  default: ({ data: _data, isCompact, height }: { data: TrendItem[]; isCompact: boolean; height: number }) => (
    <div data-testid="trend-chart" data-compact={isCompact} data-height={height}>
      TrendChart
    </div>
  ),
  TrendChart: ({ data: _data, isCompact, height }: { data: TrendItem[]; isCompact: boolean; height: number }) => (
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
  pendingReports: 3,
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
  { categoryId: 1, name: '电子产品', parentId: null, parentName: null, level: 0, sortOrder: 1, status: 1, productCount: 100, createTime: null, updateTime: null },
  { categoryId: 2, name: '图书', parentId: null, parentName: null, level: 0, sortOrder: 2, status: 1, productCount: 50, createTime: null, updateTime: null },
  { categoryId: 3, name: '服装', parentId: null, parentName: null, level: 0, sortOrder: 3, status: 0, productCount: 30, createTime: null, updateTime: null },
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
    trend: TrendItem[];
    activity: ActivityItem[];
  }> = {},
) {
  const {
    stats = sampleStats,
    statsLoading = false,
    categories = sampleCategories,
    categoriesLoading = false,
    orderStats = sampleOrderStats,
    trend = sampleTrend,
    activity = sampleActivity,
  } = overrides;

  mockUseDashboardStats.mockReturnValue({ data: stats, isLoading: statsLoading });
  mockUseAdminCategories.mockReturnValue({ data: categories, isLoading: categoriesLoading });
  mockUseAdminOrderStats.mockReturnValue({ data: orderStats });
  mockUseTrend.mockReturnValue({ data: trend });
  mockUseRecentActivity.mockReturnValue({ data: activity });
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
    expect(screen.getByText('平台运营数据概览与趋势分析')).toBeInTheDocument();
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

    // Values
    expect(screen.getByText('15')).toBeInTheDocument(); // todayOrders
    expect(screen.getByText('10')).toBeInTheDocument(); // toShip
    expect(screen.getByText('8')).toBeInTheDocument(); // toReceive
    expect(screen.getByText('250')).toBeInTheDocument(); // completed
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

    expect(screen.getByText('暂无分类数据')).toBeInTheDocument();
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

    expect(screen.getByText('暂无动态')).toBeInTheDocument();
  });

  // ── Test 11: Order stats null/undefined ──
  it('handles missing order stats', () => {
    setupMocks({ orderStats: undefined });
    renderWithProviders(<StatsPage />);

    expect(screen.queryByText('今日订单')).not.toBeInTheDocument();
    expect(screen.queryByText('待发货')).not.toBeInTheDocument();
  });
});
