import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { server } from '@/testUtils/mocks/server';
import { useDashboardStats, useRecentActivity, useTrend } from './useAdminDashboard';

const testQc = new QueryClient({
    defaultOptions: {
        queries: { retry: false, gcTime: 0 },
    },
});

function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}

describe('useDashboardStats', () => {
    it('returns dashboard stats', async () => {
        server.use(
            http.get('/api/admin/dashboard/stats', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { totalUsers: 100, totalProducts: 200, totalOrders: 50, totalRevenue: 10000 },
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useDashboardStats(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.totalUsers).toBe(100);
        expect(result.current.data?.totalProducts).toBe(200);
        expect(result.current.data?.totalOrders).toBe(50);
        expect(result.current.data?.totalRevenue).toBe(10000);
    });
});

describe('useTrend', () => {
    it('returns trend data', async () => {
        server.use(
            http.get('/api/admin/dashboard/trend', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: [{ month: '2026-05', users: 10, products: 5, orders: 3 }],
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useTrend(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toHaveLength(1);
        expect(result.current.data?.[0]?.month).toBe('2026-05');
        expect(result.current.data?.[0]?.users).toBe(10);
    });
});

describe('useRecentActivity', () => {
    it('returns activity items', async () => {
        server.use(
            http.get('/api/admin/dashboard/activity', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: [{ time: '2026-05-16 12:00:00', text: 'LOGIN', type: 'user' }],
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useRecentActivity(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toHaveLength(1);
        expect(result.current.data?.[0]?.text).toBe('LOGIN');
    });
});
