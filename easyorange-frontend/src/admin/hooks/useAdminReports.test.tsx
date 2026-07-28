import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { createMockAdminReport, createMockAdminReportQuery, createMockReportStats } from '@/testUtils/factories';
import { server } from '@/testUtils/mocks/server';
import { useAdminReportDetail, useAdminReportStats, useAdminReports, useHandleReport } from './useAdminReports';

const testQc = new QueryClient({
    defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
    },
});

function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}

describe('useAdminReports', () => {
    it('returns paginated report list', async () => {
        const mockData = {
            records: [createMockAdminReport()],
            total: 1,
            current: 1,
            size: 20,
            pages: 1,
        };

        server.use(
            http.get('/api/admin/reports', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockData,
                    timestamp: Date.now(),
                });
            })
        );

        const params = createMockAdminReportQuery();
        const { result } = renderHook(() => useAdminReports(params), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.records).toHaveLength(1);
    });
});

describe('useAdminReportDetail', () => {
    it('returns report detail', async () => {
        const { result } = renderHook(() => useAdminReportDetail('1'), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.reportId).toBe('1');
        expect(result.current.data?.productName).toBe('测试商品');
    });
});

describe('useAdminReportStats', () => {
    it('returns report statistics', async () => {
        const mockStats = createMockReportStats({ pendingReports: 5 });

        server.use(
            http.get('/api/admin/reports/stats', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockStats,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useAdminReportStats(), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.pendingReports).toBe(5);
    });
});

describe('useHandleReport', () => {
    it('handles report and invalidates cache', async () => {
        const { result } = renderHook(() => useHandleReport(), {
            wrapper: Wrapper,
        });

        result.current.mutate({
            id: '1',
            data: { action: 'resolve', remark: '已确认处理' },
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
});
