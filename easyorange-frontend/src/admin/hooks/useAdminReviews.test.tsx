import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import { useAdminReviews, useAdminReviewDetail, useDeleteReview } from './useAdminReviews';
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


describe('useAdminReviews', () => {
  it('returns paginated review list', async () => {
    server.use(
      http.get('/api/admin/reviews', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: {
            records: [{ id: 'rev-1', content: '好评', rating: 5 }],
            total: 1,
            current: 1,
            size: 20,
            pages: 1,
          },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminReviews({ pageNum: 1, pageSize: 20 }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].content).toBe('好评');
  });
});

describe('useAdminReviewDetail', () => {
  it('returns review detail', async () => {
    server.use(
      http.get('/api/admin/reviews/rev-1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { reviewId: 'rev-1', content: '好评', rating: 5, productId: '1' } as const,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminReviewDetail('rev-1'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.reviewId).toBe('rev-1');
    expect(result.current.data?.rating).toBe(5);
  });
});

describe('useDeleteReview', () => {
  it('deletes review successfully', async () => {
    server.use(
      http.delete('/api/admin/reviews/rev-1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useDeleteReview(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 'rev-1', data: { reason: '违规内容' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
