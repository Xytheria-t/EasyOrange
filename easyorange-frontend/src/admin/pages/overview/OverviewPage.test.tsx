import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { adminApi } from '../../api/adminApi';
import OverviewPage from './OverviewPage';

vi.mock('../../api/adminApi', () => ({
    adminApi: {
        getDashboardStats: vi.fn(),
        getTrend: vi.fn(),
        getActivity: vi.fn(),
        getOrderStats: vi.fn(),
        getCategories: vi.fn(),
    },
}));

const ok = <T,>(data: T) => ({ code: 'A0000' as const, message: 'ok', data, timestamp: Date.now() });

function mockAll() {
    vi.mocked(adminApi.getDashboardStats).mockResolvedValue(
        ok({
            totalUsers: 128,
            todayNewUsers: 7,
            totalProducts: 42,
            pendingProducts: 5,
            totalOrders: 19,
            todayOrders: 3,
            totalRevenue: 8600.5,
        })
    );
    vi.mocked(adminApi.getOrderStats).mockResolvedValue(
        ok({
            totalOrders: 19,
            todayOrders: 3,
            pendingPayment: 1,
            toShip: 2,
            toReceive: 3,
            completed: 9,
            cancelled: 2,
            refunded: 2,
            totalRevenue: 8600.5,
            todayRevenue: 120,
        })
    );
    vi.mocked(adminApi.getTrend).mockResolvedValue(
        ok([
            { month: '2026-08', users: 10, products: 5, orders: 2 },
            { month: '2026-09', users: 14, products: 8, orders: 4 },
        ])
    );
    vi.mocked(adminApi.getActivity).mockResolvedValue(
        ok([{ time: '2026-10-01 10:00:00', text: '新用户注册', type: 'user' }])
    );
    vi.mocked(adminApi.getCategories).mockResolvedValue(
        ok([
            {
                id: 'c1',
                name: '电子产品',
                parentId: null,
                parentName: null,
                level: 1,
                sortOrder: 1,
                status: 1,
                productCount: 10,
                createTime: null,
                children: [],
            },
            {
                id: 'c2',
                name: '图书',
                parentId: null,
                parentName: null,
                level: 1,
                sortOrder: 2,
                status: 1,
                productCount: 4,
                createTime: null,
                children: [],
            },
        ])
    );
}

describe('OverviewPage (数据总览)', () => {
    it('渲染四个核心指标', async () => {
        mockAll();
        renderWithProviders(<OverviewPage />);

        await waitFor(() => expect(screen.getByText('总用户数')).toBeInTheDocument());
        expect(screen.getByText('总商品数')).toBeInTheDocument();
        expect(screen.getByText('总订单数')).toBeInTheDocument();
        // 「累计营收」在 KPI 卡与订单分布条里各出现一次，按 KPI 标签收敛
        expect(screen.getByText('累计营收', { selector: '.admin-kpi-label' })).toBeInTheDocument();
        expect(screen.getByText('128')).toBeInTheDocument();
        // 副指标指向下一步动作：待审核 5 件
        expect(screen.getByText('待审核 5 件')).toBeInTheDocument();
    });

    it('订单状态分布按状态计数', async () => {
        mockAll();
        renderWithProviders(<OverviewPage />);

        await waitFor(() => expect(screen.getByText('待付款')).toBeInTheDocument());
        const metric = screen.getByText('待付款').closest('.admin-metric');
        expect(metric).toHaveTextContent('1');
        expect(screen.getByText('已完成').closest('.admin-metric')).toHaveTextContent('9');
    });

    it('分类分布只算一级分类并给出占比', async () => {
        mockAll();
        renderWithProviders(<OverviewPage />);

        await waitFor(() => expect(screen.getByText('电子产品')).toBeInTheDocument());
        expect(screen.getByText('10 件 · 71.4%')).toBeInTheDocument();
        expect(screen.getByText(/共 2 个一级分类/)).toBeInTheDocument();
    });

    it('走势按月画折线并给环比', async () => {
        mockAll();
        renderWithProviders(<OverviewPage />);

        await waitFor(() => expect(screen.getByText('用户新增')).toBeInTheDocument());
        // 10 → 14，环比 +40%
        expect(screen.getByText('+40%')).toBeInTheDocument();
        expect(document.querySelectorAll('polyline').length).toBeGreaterThan(0);
    });

    it('最近动态列出动态条目', async () => {
        mockAll();
        renderWithProviders(<OverviewPage />);

        await waitFor(() => expect(screen.getByText('新用户注册')).toBeInTheDocument());
    });
});
