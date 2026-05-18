import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import {
  useAdminOrders,
  useAdminOrderDetail,
  useAdminOrderStats,
  useCancelOrder,
  useForceCompleteOrder,
  useRefundOrder,
} from './useAdminOrders';
import type { ReactNode } from 'react';

const testQc = new QueryClient({
  defaultOptions: {
    queries: { retry: false, gcTime: 0 },
    mutations: { retry: false },
  },
});

function Wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}


describe('useAdminOrders', () => {
  it('returns paginated order list', async () => {
    server.use(
      http.get('/api/admin/orders', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: {
            records: [{ id: 1, orderNo: 'ORD001', status: 1 }],
            total: 1,
            current: 1,
            size: 20,
            pages: 1,
          },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminOrders({ pageNum: 1, pageSize: 20 }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].orderNo).toBe('ORD001');
  });
});

describe('useAdminOrderDetail', () => {
  it('returns order detail', async () => {
    server.use(
      http.get('/api/admin/orders/1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: 1, orderNo: 'ORD001', status: 1 },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminOrderDetail(1), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.orderNo).toBe('ORD001');
  });
});

describe('useAdminOrderStats', () => {
  it('returns order statistics', async () => {
    server.use(
      http.get('/api/admin/orders/stats', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { totalOrders: 100, pendingPayment: 5, toShip: 3, toReceive: 2, completed: 80, cancelled: 5, refunded: 2, todayOrders: 10, totalRevenue: 10000, todayRevenue: 1000 },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminOrderStats(), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalOrders).toBe(100);
    expect(result.current.data?.pendingPayment).toBe(5);
  });
});

describe('useCancelOrder', () => {
  it('cancels order successfully', async () => {
    server.use(
      http.put('/api/admin/orders/1/cancel', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useCancelOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 1, data: { reason: '买家要求' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useForceCompleteOrder', () => {
  it('force completes order successfully', async () => {
    server.use(
      http.put('/api/admin/orders/1/force-complete', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useForceCompleteOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 1, data: { reason: '特殊处理' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useRefundOrder', () => {
  it('refunds order successfully', async () => {
    server.use(
      http.put('/api/admin/orders/1/refund', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useRefundOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 1, data: { reason: '退款处理' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
