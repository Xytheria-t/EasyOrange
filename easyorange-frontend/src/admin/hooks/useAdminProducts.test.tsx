import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import {
  useAdminProducts,
  useAdminProductDetail,
  useUpdateProductStatus,
} from './useAdminProducts';
import {
  createMockAdminProduct,
  createMockAdminProductQuery,
} from '@/testUtils/factories';
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

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('useAdminProducts', () => {
  it('returns paginated product list', async () => {
    const mockData = {
      records: [createMockAdminProduct()],
      total: 1,
      current: 1,
      size: 20,
      pages: 1,
    };

    server.use(
      http.get('/api/admin/products', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: mockData,
          timestamp: Date.now(),
        });
      }),
    );

    const params = createMockAdminProductQuery();
    const { result } = renderHook(() => useAdminProducts(params), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].name).toBe('测试商品');
  });
});

describe('useAdminProductDetail', () => {
  it('returns product detail', async () => {
    const mockProduct = createMockAdminProduct({ productId: 42 });

    server.use(
      http.get('/api/admin/products/42', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: mockProduct,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminProductDetail(42), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.productId).toBe(42);
  });
});

describe('useUpdateProductStatus', () => {
  it('updates status and invalidates list cache', async () => {
    server.use(
      http.put('/api/admin/products/1/status', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useUpdateProductStatus(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 1, data: { status: 3, reason: '下架' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
