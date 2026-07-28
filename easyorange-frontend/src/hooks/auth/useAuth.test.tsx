import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/store';
import { server } from '@/testUtils/mocks/server';
import { useCurrentUser, useLogin, useLogout, useRegister } from './useAuth';

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
    });
});

describe('useCurrentUser', () => {
    it('fetches current user when token exists', async () => {
        useAuthStore.setState({ token: 'test-token' });

        server.use(
            http.get('/api/users/me', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { id: '1', username: 'testuser', nickname: '测试用户' },
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useCurrentUser(), {
            wrapper: Wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data?.username).toBe('testuser');
        expect(result.current.data?.nickname).toBe('测试用户');
    });

    it('does not fetch when token is missing', () => {
        const { result } = renderHook(() => useCurrentUser(), {
            wrapper: Wrapper,
        });

        expect(result.current.fetchStatus).toBe('idle');
    });
});

describe('useLogin', () => {
    it('logs in successfully and updates store', async () => {
        const mockUser = { id: '1', username: 'testuser', nickname: '测试用户' };
        const mockToken = 'access-token-123';
        const mockRefreshToken = 'refresh-token-456';

        server.use(
            http.post('/api/auth/login', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { user: mockUser, accessToken: mockToken, refreshToken: mockRefreshToken },
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useLogin(), {
            wrapper: Wrapper,
        });

        result.current.mutate({ account: 'testuser', password: 'password123', loginMethod: 'password' });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        const state = useAuthStore.getState();
        expect(state.token).toBe(mockToken);
        expect(state.refreshToken).toBe(mockRefreshToken);
        expect(state.user).toEqual(mockUser);
        expect(!!state.token).toBe(true);
    });
});

describe('useRegister', () => {
    it('registers successfully', async () => {
        server.use(
            http.post('/api/auth/register', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: 2,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useRegister(), {
            wrapper: Wrapper,
        });

        result.current.mutate({
            username: 'newuser',
            password: 'password123',
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toBe(2);
    });
});

describe('useLogout', () => {
    it('logs out successfully and clears auth', async () => {
        useAuthStore.setState({
            token: 'test-token',
            user: { id: '1' } as unknown as import('@/types').User,
        });

        server.use(
            http.post('/api/auth/logout', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: null,
                    timestamp: Date.now(),
                });
            })
        );

        const { result } = renderHook(() => useLogout(), {
            wrapper: Wrapper,
        });

        const logout = result.current;
        await logout();

        const state = useAuthStore.getState();
        expect(state.token).toBeNull();
        expect(state.user).toBeNull();
    });
});
