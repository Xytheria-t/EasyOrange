import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { clearCache } from '@/api/core/cache';
import { server } from '@/testUtils/mocks/server';
import { usePlatformStats } from './useStats';

function Wrapper({ children }: { children: ReactNode }) {
    const qc = new QueryClient({
        defaultOptions: {
            queries: { retry: false, gcTime: 0 },
        },
    });
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

afterEach(() => {
    server.resetHandlers();
    clearCache();
});

describe('usePlatformStats', () => {
    it('returns platform stats', async () => {
        server.use(
            http.get('/api/stats/platform', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { activeUsers: 100, onlineProducts: 50, completedOrders: 30 },
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => usePlatformStats(), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.activeUsers).toBe(100);
        expect(result.current.data?.onlineProducts).toBe(50);
        expect(result.current.data?.completedOrders).toBe(30);
    });

    it('returns default values when API returns null data', async () => {
        server.use(
            http.get('/api/stats/platform', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => usePlatformStats(), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.activeUsers).toBe(0);
        expect(result.current.data?.onlineProducts).toBe(0);
        expect(result.current.data?.completedOrders).toBe(0);
    });
});
