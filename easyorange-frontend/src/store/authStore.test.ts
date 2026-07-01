import { beforeEach, describe, expect, it } from 'vitest';
import type { User } from '@/types';
import { useAuthStore } from './authStore';

beforeEach(() => {
    useAuthStore.setState({
        user: null,
        token: null,
        refreshToken: null,
    });
});

describe('authStore', () => {
    describe('initial state', () => {
        it('starts with no user and no token', () => {
            const state = useAuthStore.getState();
            expect(state.user).toBeNull();
            expect(state.token).toBeNull();
            expect(state.refreshToken).toBeNull();
        });
    });

    describe('setUser', () => {
        it('updates user', () => {
            const mockUser = { id: '1', username: 'test', nickname: 'Test' } as unknown as User;
            useAuthStore.getState().setUser(mockUser);
            expect(useAuthStore.getState().user).toEqual(mockUser);
        });

        it('clears user when set to null', () => {
            useAuthStore.getState().setUser({ id: '1' } as unknown as User);
            useAuthStore.getState().setUser(null);
            expect(useAuthStore.getState().user).toBeNull();
        });
    });

    describe('setToken', () => {
        it('updates token', () => {
            useAuthStore.getState().setToken('my-token');
            expect(useAuthStore.getState().token).toBe('my-token');
        });

        it('clears token when set to null', () => {
            useAuthStore.setState({ token: 'existing-token' });
            useAuthStore.getState().setToken(null);
            expect(useAuthStore.getState().token).toBeNull();
        });
    });

    describe('login', () => {
        it('sets user, token and refreshToken', () => {
            const mockUser = { id: '1', username: 'test' } as unknown as User;
            useAuthStore.getState().login(mockUser, 'access-token', 'refresh-token');
            const state = useAuthStore.getState();
            expect(state.user).toEqual(mockUser);
            expect(state.token).toBe('access-token');
            expect(state.refreshToken).toBe('refresh-token');
        });
    });

    describe('logout', () => {
        it('clears all session data', () => {
            useAuthStore.getState().login({ id: '1' } as unknown as User, 'token', 'refresh');
            useAuthStore.getState().logout();
            const state = useAuthStore.getState();
            expect(state.user).toBeNull();
            expect(state.token).toBeNull();
            expect(state.refreshToken).toBeNull();
        });
    });
});
