import { HttpResponse, http } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '@/store';
import { server } from '@/testUtils/mocks/server';
import { clearSession, refreshAccessToken, restoreSession, setSession } from './session';

afterEach(() => {
    server.resetHandlers();
    useAuthStore.setState({ user: null, token: null });
});

describe('refreshAccessToken', () => {
    it('从 HttpOnly cookie 刷新 access token 并写入 store', async () => {
        server.use(
            http.post('/api/auth/refresh', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { accessToken: 'new-access-token' },
                    timestamp: Date.now(),
                });
            })
        );

        const token = await refreshAccessToken();

        expect(token).toBe('new-access-token');
        expect(useAuthStore.getState().token).toBe('new-access-token');
    });

    it('刷新失败时清空会话并返回 null', async () => {
        server.use(
            http.post('/api/auth/refresh', () => {
                return HttpResponse.json({ code: 'A0401', message: '刷新令牌已失效', data: null }, { status: 401 });
            })
        );
        useAuthStore.setState({ token: 'stale-token' });

        const token = await refreshAccessToken();

        expect(token).toBeNull();
        expect(useAuthStore.getState().token).toBeNull();
    });
});

describe('restoreSession', () => {
    it('无 token 时经 refresh + 拉用户恢复会话', async () => {
        const mockUser = { id: '1', username: 'testuser', nickname: '测试用户' };
        server.use(
            http.post('/api/auth/refresh', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { accessToken: 'fresh-access' },
                    timestamp: Date.now(),
                });
            }),
            http.get('/api/users/me', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: mockUser,
                    timestamp: Date.now(),
                });
            })
        );

        await restoreSession();

        const state = useAuthStore.getState();
        expect(state.token).toBe('fresh-access');
        expect(state.user).toEqual(mockUser);
    });

    it('refresh 失败时清空会话', async () => {
        server.use(
            http.post('/api/auth/refresh', () => {
                return HttpResponse.json({ code: 'A0401', message: '刷新令牌已失效', data: null }, { status: 401 });
            })
        );

        await restoreSession();

        expect(useAuthStore.getState().token).toBeNull();
        expect(useAuthStore.getState().user).toBeNull();
    });
});

describe('setSession / clearSession', () => {
    it('setSession 写入 token 与 user（不含 refreshToken）', () => {
        const user = { id: '1', username: 'u' } as unknown as import('@/types').User;
        setSession('access', user);

        const state = useAuthStore.getState();
        expect(state.token).toBe('access');
        expect(state.user).toEqual(user);
        expect('refreshToken' in state).toBe(false);
    });

    it('clearSession 清空会话', () => {
        useAuthStore.setState({ token: 't', user: { id: '1' } as unknown as import('@/types').User });
        clearSession('logout');

        expect(useAuthStore.getState().token).toBeNull();
        expect(useAuthStore.getState().user).toBeNull();
    });
});
