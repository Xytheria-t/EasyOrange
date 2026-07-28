import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { server } from '@/testUtils/mocks/server';
import {
    useAdminCategories,
    useAdminCategoryTree,
    useCreateCategory,
    useDeleteCategory,
    useUpdateCategory,
} from './useAdminCategories';

const testQc = new QueryClient({
    defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
    },
});

function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}

describe('useAdminCategories', () => {
    it('returns category list', async () => {
        server.use(
            http.get('/api/admin/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: [{ id: 1, name: '电子产品', status: 1, sort: 1, parentId: '0' }],
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useAdminCategories(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toHaveLength(1);
        expect(result.current.data?.[0]?.name).toBe('电子产品');
    });
});

describe('useAdminCategoryTree', () => {
    it('returns category tree', async () => {
        server.use(
            http.get('/api/admin/categories/tree', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: [{ id: 1, name: '电子产品', children: [] }],
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useAdminCategoryTree(), { wrapper: Wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toHaveLength(1);
        expect(result.current.data?.[0]?.name).toBe('电子产品');
    });
});

describe('useCreateCategory', () => {
    it('creates category successfully', async () => {
        server.use(
            http.post('/api/admin/categories', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: 1,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useCreateCategory(), { wrapper: Wrapper });

        result.current.mutate({ name: '新分类', parentId: '0', sortOrder: 1 });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toBe(1);
    });
});

describe('useUpdateCategory', () => {
    it('updates category successfully', async () => {
        server.use(
            http.put('/api/admin/categories/1', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useUpdateCategory(), { wrapper: Wrapper });

        result.current.mutate({ id: '1', data: { name: '更新名', sortOrder: 2 } });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
});

describe('useDeleteCategory', () => {
    it('deletes category successfully', async () => {
        server.use(
            http.delete('/api/admin/categories/1', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useDeleteCategory(), { wrapper: Wrapper });

        result.current.mutate('1');

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
});
