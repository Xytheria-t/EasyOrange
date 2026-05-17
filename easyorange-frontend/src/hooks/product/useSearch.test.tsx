import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import { useProductSearch, useSearchSuggestions, useHotKeywords } from './useSearch';
import type { ReactNode } from 'react';

const testQc = new QueryClient({
  defaultOptions: { queries: { retry: false, gcTime: 0 } },
});

function Wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}


describe('useProductSearch', () => {
  it('returns search results', async () => {
    server.use(
      http.get('/api/products/search', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: {
            records: [],
            total: 0,
            pageNum: 1,
            pageSize: 20,
            pages: 0,
          },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useProductSearch('手机'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toEqual([]);
  });

  it('is not enabled when keyword is empty', () => {
    const { result } = renderHook(() => useProductSearch('  '), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useSearchSuggestions', () => {
  it('returns suggestions', async () => {
    server.use(
      http.get('/api/products/search/suggestions', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: ['手机', '手机壳', '手机膜'],
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useSearchSuggestions('手机'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(['手机', '手机壳', '手机膜']);
  });

  it('is not enabled for short keyword (< 2 chars)', () => {
    const { result } = renderHook(() => useSearchSuggestions('手'), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useHotKeywords', () => {
  it('returns hot keywords', async () => {
    server.use(
      http.get('/api/products/search/hot', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: [
            { keyword: '手机', searchCount: 100 },
            { keyword: '电脑', searchCount: 80 },
          ],
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useHotKeywords(5), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(2);
    expect(result.current.data![0].keyword).toBe('手机');
    expect(result.current.data![0].searchCount).toBe(100);
  });
});
