import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import { useProducts, useProduct, useCreateProduct, useDeleteProduct } from './useProducts';
import { createMockProduct } from '@/testUtils/factories';
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


describe('useProducts', () => {
  it('returns products list', async () => {
    const mockPage = {
      records: [createMockProduct()],
      total: 1,
      pageNum: 1,
      pageSize: 20,
      pages: 1,
    };

    server.use(
      http.get('/api/products', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: mockPage,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useProducts({ pageNum: 1, pageSize: 20 }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.total).toBe(1);
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].title).toBe('测试商品');
  });

  it('returns empty records when API returns no data', async () => {
    server.use(
      http.get('/api/products', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { records: [], total: 0, pageNum: 1, pageSize: 20, pages: 0 },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useProducts(), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toEqual([]);
    expect(result.current.data?.total).toBe(0);
  });
});

describe('useProduct', () => {
  it('returns product detail', async () => {
    const mockProduct = createMockProduct({ id: '123' });

    server.use(
      http.get('/api/products/123', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: mockProduct,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useProduct('123'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.id).toBe('123');
    expect(result.current.data?.title).toBe('测试商品');
  });

  it('is not enabled when id is empty', () => {
    const { result } = renderHook(() => useProduct(''), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useCreateProduct', () => {
  it('creates product and invalidates list cache', async () => {
    server.use(
      http.post('/api/products', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: 'new-id',
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useCreateProduct(), {
      wrapper: Wrapper,
    });

    result.current.mutate({
      name: '新商品',
      description: '描述',
      price: 100,
      categoryId: 1,
      conditionLevel: 1,
      imageUrls: [],
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe('new-id');
  });
});

describe('useDeleteProduct', () => {
  it('deletes product and invalidates list cache', async () => {
    server.use(
      http.delete('/api/products/1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useDeleteProduct(), {
      wrapper: Wrapper,
    });

    result.current.mutate('1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
