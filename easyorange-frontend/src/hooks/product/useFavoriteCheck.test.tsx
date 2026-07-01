import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { useAuthStore, useUIStore } from '@/store';
import { server } from '@/testUtils/mocks/server';
import { useFavoriteCheck } from './useFavoriteCheck';

const testQc = new QueryClient({
    defaultOptions: {
        queries: { retry: false, gcTime: 0 },
        mutations: { retry: false },
    },
});

function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}

afterEach(() => {
    server.resetHandlers();
    useAuthStore.setState({
        user: null,
        token: null,
        refreshToken: null,
        isAuthenticated: false,
    });
    useUIStore.setState({ toasts: [], isLoading: false, loadingMessage: '' });
});

describe('useFavoriteCheck', () => {
    describe('checkFavorites', () => {
        it('fetches favorite status when logged in', async () => {
            useAuthStore.setState({
                token: 'test-token',
                user: { id: '1' } as unknown as import('@/types').User,
                isAuthenticated: true,
            });

            server.use(
                http.post('/api/favorites/batch-check', () => {
                    return HttpResponse.json({
                        code: 'A0000',
                        message: 'success',
                        data: { '1': true, '2': false },
                        timestamp: Date.now(),
                    });
                })
            );

            const { result } = renderHook(() => useFavoriteCheck(), { wrapper: Wrapper });

            await act(async () => {
                await result.current.checkFavorites(['1', '2']);
            });

            expect(result.current.isFavorited('1')).toBe(true);
            expect(result.current.isFavorited('2')).toBe(false);
        });

        it('returns empty map when not logged in', async () => {
            const { result } = renderHook(() => useFavoriteCheck(), { wrapper: Wrapper });

            await act(async () => {
                await result.current.checkFavorites(['1', '2']);
            });

            expect(result.current.isFavorited('1')).toBe(false);
        });
    });

    describe('toggleFavorite', () => {
        it('adds favorite successfully', async () => {
            useAuthStore.setState({
                token: 'test-token',
                user: { id: '1' } as unknown as import('@/types').User,
                isAuthenticated: true,
            });

            server.use(
                http.post('/api/favorites/1', () => {
                    return HttpResponse.json({
                        code: 'A0000',
                        message: 'success',
                        data: null,
                        timestamp: Date.now(),
                    });
                })
            );

            const { result } = renderHook(() => useFavoriteCheck(), { wrapper: Wrapper });

            let success = false;
            await act(async () => {
                success = await result.current.toggleFavorite('1', true);
            });

            expect(success).toBe(true);
            expect(result.current.isFavorited('1')).toBe(true);
        });

        it('removes favorite successfully', async () => {
            useAuthStore.setState({
                token: 'test-token',
                user: { id: '1' } as unknown as import('@/types').User,
                isAuthenticated: true,
            });

            server.use(
                http.delete('/api/favorites/1', () => {
                    return HttpResponse.json({
                        code: 'A0000',
                        message: 'success',
                        data: null,
                        timestamp: Date.now(),
                    });
                })
            );

            const { result } = renderHook(() => useFavoriteCheck(), { wrapper: Wrapper });

            let success = false;
            await act(async () => {
                success = await result.current.toggleFavorite('1', false);
            });

            expect(success).toBe(true);
            expect(result.current.isFavorited('1')).toBe(false);
        });

        it('returns false when not logged in', async () => {
            const { result } = renderHook(() => useFavoriteCheck(), { wrapper: Wrapper });

            let success = true;
            await act(async () => {
                success = await result.current.toggleFavorite('1', true);
            });

            expect(success).toBe(false);
        });
    });
});
