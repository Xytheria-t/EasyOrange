import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '@/types';

interface AuthState {
    user: User | null;
    token: string | null;
    refreshToken: string | null;
    setUser: (user: User | null) => void;
    setToken: (token: string | null) => void;
    login: (user: User, token: string, refreshToken: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            user: null,
            token: null,
            refreshToken: null,

            setUser: (user) => set({ user }),

            setToken: (token) => set({ token }),

            login: (user, token, refreshToken) => set({
                user,
                token,
                refreshToken,
            }),

            logout: () => set({
                user: null,
                token: null,
                refreshToken: null,
            }),
        }),
        {
            name: 'auth-storage',
            storage: createJSONStorage(() => sessionStorage),
            partialize: (state) => ({
                token: state.token,
                refreshToken: state.refreshToken,
                user: state.user,
            }),
        }
    )
);

/** 是否已认证（派生状态，从 token 存在性判断） */
export const useIsAuthenticated = () => useAuthStore((state) => !!state.token);
