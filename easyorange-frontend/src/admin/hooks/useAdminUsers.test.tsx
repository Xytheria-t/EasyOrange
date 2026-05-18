import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import { useAdminUsers, useAdminUserDetail, useUpdateUserStatus } from './useAdminUsers';
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


describe('useAdminUsers', () => {
  it('returns paginated user list', async () => {
    server.use(
      http.get('/api/admin/users', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: {
            records: [{ id: 1, username: 'admin', userType: '00', nickname: '管理员' }],
            total: 1,
            current: 1,
            size: 20,
            pages: 1,
          },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminUsers({ pageNum: 1, pageSize: 20 }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.records).toHaveLength(1);
    expect(result.current.data?.records[0].username).toBe('admin');
  });
});

describe('useAdminUserDetail', () => {
  it('returns user detail', async () => {
    server.use(
      http.get('/api/admin/users/1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: 1, username: 'admin', email: 'admin@test.com', userType: '00' },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useAdminUserDetail(1), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.userId).toBe(1);
    expect(result.current.data?.username).toBe('admin');
  });

  it('is not enabled when id is 0', () => {
    const { result } = renderHook(() => useAdminUserDetail(0), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useUpdateUserStatus', () => {
  it('updates user status', async () => {
    server.use(
      http.put('/api/admin/users/1/status', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useUpdateUserStatus(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ id: 1, data: { status: 1 } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
