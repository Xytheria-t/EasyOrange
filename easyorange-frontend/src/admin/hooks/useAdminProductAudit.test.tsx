import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { server } from '@/testUtils/mocks/server';
import { useAuditLogs, useAuditProduct, useBatchAuditProducts } from './useAdminProductAudit';

const testQc = new QueryClient({
    defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
    },
});

function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}

describe('useAuditProduct', () => {
    it('audits a product and invalidates list cache', async () => {
        server.use(
            http.put('/api/admin/products/1/audit', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useAuditProduct(), {
            wrapper: Wrapper,
        });

        result.current.mutate({
            id: 1,
            data: { action: 1 },
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
});

describe('useBatchAuditProducts', () => {
    it('batch audits products', async () => {
        server.use(
            http.put('/api/admin/products/batch-audit', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useBatchAuditProducts(), {
            wrapper: Wrapper,
        });

        result.current.mutate({
            items: [
                { productId: 1, action: 1 },
                { productId: 2, action: 2, reason: '图片不合规' },
            ],
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
});

describe('useAuditLogs', () => {
    it('fetches audit logs for product', async () => {
        server.use(
            http.get('/api/admin/products/1/audit-logs', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: [
                        {
                            id: 1,
                            productId: 1,
                            operatorId: 1,
                            operatorName: '管理员',
                            action: 1,
                            actionDesc: '审核通过',
                            reason: null,
                            dimensions: ['basic', 'image'],
                            beforeStatus: 4,
                            beforeStatusDesc: '待审核',
                            afterStatus: 1,
                            afterStatusDesc: '在售',
                            remark: null,
                            createTime: '2026-05-16 12:00:00',
                        },
                    ],
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useAuditLogs(1), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toHaveLength(1);
        expect(result.current.data?.[0]?.action).toBe(1);
        expect(result.current.data?.[0]?.actionDesc).toBe('审核通过');
    });

    it('is not enabled when productId is null', () => {
        const { result } = renderHook(() => useAuditLogs(null), {
            wrapper: Wrapper,
        });

        expect(result.current.fetchStatus).toBe('idle');
    });
});
