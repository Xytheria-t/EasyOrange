import { request } from '@/api/core/request';
import { useAuthStore } from '@/store/authStore';
import type { TokenRefreshResult } from '@/types';

import type { UserType } from '@/types/user';

const LOGIN_GRACE_PERIOD_MS = 5000;

const EMPTY_USER_FALLBACK: import('@/types').User = {
    userId: '',
    username: '',
    nickname: '',
    email: '',
    phone: null,
    studentId: null,
    realName: null,
    avatar: null,
    status: 0,
    userType: '01' as UserType,
    createTime: '',
    updateTime: '',
};

export const AUTH_SESSION_CHANGE_EVENT = 'auth-session-change';

export type AuthSessionClearReason = 'logout' | 'unauthorized' | 'manual';

export interface AuthSessionDetail {
    isAuthenticated: boolean;
    token: string | null;
    reason?: AuthSessionClearReason;
}

// ==================== Token Refresh 管理 ====================

class RefreshCoordinator {
    private isRefreshing = false;
    private pendingCallbacks: Array<(token: string) => void> = [];
    private lastLoginTimestamp = 0;
    private unauthorizedRedirectInFlight = false;

    /** 等待正在进行的刷新完成 */
    waitForRefresh(): Promise<string | null> {
        return new Promise((resolve) => {
            this.pendingCallbacks.push(resolve);
        });
    }

    notifyPending(token: string): void {
        this.pendingCallbacks.forEach((cb) => cb(token));
        this.pendingCallbacks = [];
    }

    getIsRefreshing(): boolean {
        return this.isRefreshing;
    }

    setIsRefreshing(value: boolean): void {
        this.isRefreshing = value;
    }

    markLogin(): void {
        this.lastLoginTimestamp = Date.now();
        this.unauthorizedRedirectInFlight = false;
    }

    isWithinGracePeriod(): boolean {
        return (
            this.lastLoginTimestamp > 0 &&
            Date.now() - this.lastLoginTimestamp < LOGIN_GRACE_PERIOD_MS
        );
    }

    isUnauthorizedRedirectInFlight(): boolean {
        return this.unauthorizedRedirectInFlight;
    }

    setUnauthorizedRedirectInFlight(value: boolean): void {
        this.unauthorizedRedirectInFlight = value;
    }
}

const refreshCoordinator = new RefreshCoordinator();

// ==================== 事件 ====================

function emitSessionChange(reason?: AuthSessionClearReason): void {
    const store = useAuthStore.getState();
    window.dispatchEvent(
        new CustomEvent<AuthSessionDetail>(AUTH_SESSION_CHANGE_EVENT, {
            detail: {
                isAuthenticated: !!store.token,
                token: store.token,
                reason,
            },
        }),
    );
}

// ==================== 导出函数 ====================

export function getStoredToken(): string | null {
    return useAuthStore.getState().token;
}

export function getStoredUser() {
    return useAuthStore.getState().user;
}

export async function refreshAccessToken(): Promise<string | null> {
    const currentRefreshToken = useAuthStore.getState().refreshToken;
    if (!currentRefreshToken) {
        return null;
    }

    // 并发请求复用：正在刷新时等待结果
    if (refreshCoordinator.getIsRefreshing()) {
        return refreshCoordinator.waitForRefresh();
    }

    refreshCoordinator.setIsRefreshing(true);

    try {
        const response = await request<TokenRefreshResult>('/auth/refresh', {
            method: 'POST',
            body: { refreshToken: currentRefreshToken },
            skipAuth: true,
            timeout: 8000,
            retries: 0,
            dedupe: false,
        });

        if (!response.data) {
            throw new Error('Refresh response invalid');
        }

        const { accessToken, refreshToken: newRefreshToken } = response.data;
        const user = useAuthStore.getState().user;

        // 更新 store（Zustand 是唯一数据源）
        useAuthStore.getState().login(
            user ?? EMPTY_USER_FALLBACK,
            accessToken,
            newRefreshToken,
        );

        refreshCoordinator.notifyPending(accessToken);
        emitSessionChange();
        return accessToken;
    } catch {
        clearSession('unauthorized');
        return null;
    } finally {
        refreshCoordinator.setIsRefreshing(false);
    }
}

export function setSession(
    token: string,
    user: import('@/types').User,
    refreshToken: string,
): void {
    useAuthStore.getState().login(user, token, refreshToken);
    refreshCoordinator.markLogin();
    emitSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    useAuthStore.getState().logout();
    emitSessionChange(reason);
}

export async function logout(): Promise<void> {
    const refreshToken = useAuthStore.getState().refreshToken;
    try {
        await request('/auth/logout', {
            method: 'POST',
            body: { refreshToken },
        });
    } catch {
        // 忽略 API 错误，确保本地状态清除
    }

    clearSession('logout');
}

export function handleUnauthorized(): void {
    if (refreshCoordinator.isUnauthorizedRedirectInFlight()) {
        return;
    }
    if (refreshCoordinator.isWithinGracePeriod()) {
        return;
    }

    refreshCoordinator.setUnauthorizedRedirectInFlight(true);
    clearSession('unauthorized');

    const currentPath = `${window.location.pathname}${window.location.search}`;
    const redirectQuery =
        currentPath && currentPath !== '/'
            ? { redirect: currentPath }
            : undefined;
    const loginPath = redirectQuery
        ? `/login?redirect=${encodeURIComponent(redirectQuery.redirect)}`
        : '/login';
    window.location.href = loginPath;
}

function isTokenExpired(token: string): boolean {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.exp * 1000 < Date.now();
    } catch {
        return true;
    }
}

/** 启动时调用，清除可能残留的过期或不完整状态 */
export function initAuth(): void {
    const store = useAuthStore.getState();
    if (!store.token) {return;}
    if (!store.refreshToken || isTokenExpired(store.token)) {
        store.logout();
    }
}
