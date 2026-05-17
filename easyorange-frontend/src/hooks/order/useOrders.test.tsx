import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import {
  useMyOrders,
  useSoldOrders,
  useOrderDetail,
  useCreateOrder,
  useCancelOrder,
  usePayOrder,
  useShipOrder,
  useReceiveOrder,
  useRefundOrder,
} from './useOrders';
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


describe('useMyOrders', () => {
  it('returns paginated my orders', async () => {
    const mockPage = {
      records: [{ id: '1', orderNo: 'ORD001', status: 1 }],
      total: 1,
      current: 1,
      size: 20,
      pages: 1,
    };

    server.use(
      http.get('/api/orders/my', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: mockPage,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useMyOrders({ pageNum: 1, pageSize: 20 }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].orderNo).toBe('ORD001');
  });

  it('returns empty records when no orders', async () => {
    server.use(
      http.get('/api/orders/my', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { records: [], total: 0, current: 1, size: 20, pages: 0 },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useMyOrders(), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toEqual([]);
  });
});

describe('useSoldOrders', () => {
  it('returns paginated sold orders', async () => {
    server.use(
      http.get('/api/orders/sold', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: {
            records: [{ id: '2', orderNo: 'ORD002', status: 2 }],
            total: 1,
            current: 1,
            size: 20,
            pages: 1,
          },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useSoldOrders(), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].orderNo).toBe('ORD002');
  });
});

describe('useOrderDetail', () => {
  it('returns order detail', async () => {
    server.use(
      http.get('/api/orders/1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: '1', orderNo: 'ORD001', status: 1 },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useOrderDetail('1'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.orderNo).toBe('ORD001');
  });

  it('is not enabled when id is empty', () => {
    const { result } = renderHook(() => useOrderDetail(''), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useCreateOrder', () => {
  it('creates order successfully', async () => {
    server.use(
      http.post('/api/orders', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: 'new-order-id' },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useCreateOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate({
      productId: '1',
      quantity: 1,
      addressId: 'addr-1',
    } as any);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.id).toBe('new-order-id');
  });
});

describe('useCancelOrder', () => {
  it('cancels order successfully', async () => {
    server.use(
      http.put('/api/orders/1/cancel', () => {
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

    result.current.mutate({ id: '1', reason: '不想要了' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('usePayOrder', () => {
  it('pays order successfully', async () => {
    server.use(
      http.put('/api/orders/1/pay', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => usePayOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate('1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useShipOrder', () => {
  it('ships order successfully', async () => {
    server.use(
      http.put('/api/orders/1/ship', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useShipOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate('1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useReceiveOrder', () => {
  it('receives order successfully', async () => {
    server.use(
      http.put('/api/orders/1/receive', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useReceiveOrder(), {
      wrapper: Wrapper,
    });

    result.current.mutate('1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useRefundOrder', () => {
  it('refunds order successfully', async () => {
    server.use(
      http.put('/api/orders/1/refund', () => {
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

    result.current.mutate({ id: '1', reason: '质量问题' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
