import { create } from 'zustand';
import type { User } from '@/types';

interface AuthState {
    user: User | null;
    token: string | null;
    setUser: (user: User | null) => void;
    setToken: (token: string | null) => void;
    login: (user: User, token: string) => void;
    logout: () => void;
}

/**
 * 认证会话状态 — access token 仅存内存（不持久化），XSS 无法窃取离线凭证。
 * 页面刷新后由 restoreSession 通过 HttpOnly refresh cookie 恢复会话。
 * refresh token 完全由 HttpOnly Cookie 持有，JS 不可见、不落 store。
 */
export const useAuthStore = create<AuthState>()(set => ({
    user: null,
    token: null,

    setUser: user => set({ user }),

    setToken: token => set({ token }),

    login: (user, token) =>
        set({
            user,
            token,
        }),

    logout: () =>
        set({
            user: null,
            token: null,
        }),
}));

/** 是否已认证（派生状态，从 token 存在性判断） */
export const useIsAuthenticated = () => useAuthStore(state => !!state.token);
