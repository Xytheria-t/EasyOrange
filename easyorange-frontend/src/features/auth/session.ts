import { request } from '@/api/core/request';
import { useAuthStore } from '@/store/authStore';
import type { TokenRefreshResult, User } from '@/types';

const LOGIN_GRACE_PERIOD_MS = 5000;

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
    private pendingCallbacks: Array<(token: string | null) => void> = [];
    private lastLoginTimestamp = 0;
    private unauthorizedRedirectInFlight = false;

    /** 等待正在进行的刷新完成 */
    waitForRefresh(): Promise<string | null> {
        return new Promise(resolve => {
            this.pendingCallbacks.push(resolve);
        });
    }

    notifyPending(token: string | null): void {
        this.pendingCallbacks.forEach(cb => {
            cb(token);
        });
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
        return this.lastLoginTimestamp > 0 && Date.now() - this.lastLoginTimestamp < LOGIN_GRACE_PERIOD_MS;
    }

    isUnauthorizedRedirectInFlight(): boolean {
        return this.unauthorizedRedirectInFlight;
    }

    setUnauthorizedRedirectInFlight(value: boolean): void {
        this.unauthorizedRedirectInFlight = value;
    }
}

const refreshCoordinator = new RefreshCoordinator();

/** 会话恢复单飞句柄 — 见 restoreSession 注释。 */
let sessionRestoreInFlight: Promise<void> | null = null;

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
        })
    );
}

// ==================== 导出函数 ====================

export function getStoredToken(): string | null {
    return useAuthStore.getState().token;
}

/** 后端随 refresh cookie 同发的非 HttpOnly 标记（见后端 RefreshCookie），无它即无 refresh 会话。 */
const REFRESH_MARKER_COOKIE = 'has_rt';

function hasRefreshMarker(): boolean {
    return document.cookie.split('; ').includes(`${REFRESH_MARKER_COOKIE}=1`);
}

/**
 * 刷新 access token。refresh token 在 HttpOnly Cookie 中，随请求自动携带，JS 不可见。
 * 并发请求复用：正在刷新时等待结果（单飞）。
 * <p>
 * 无标记 cookie 时跳过：冷启动没有 refresh 会话却盲调 /auth/refresh 必 401（TD-023 console 噪音）。
 */
export async function refreshAccessToken(): Promise<string | null> {
    if (!hasRefreshMarker()) {
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
            skipAuth: true,
            timeout: 8000,
            retries: 0,
            dedupe: false,
        });

        if (!response.data?.accessToken) {
            throw new Error('Refresh response invalid');
        }

        const accessToken = response.data.accessToken;
        useAuthStore.getState().setToken(accessToken);

        refreshCoordinator.notifyPending(accessToken);
        emitSessionChange();
        return accessToken;
    } catch {
        // 失败也要唤醒并发等待者，避免 Promise 悬挂（单飞泄漏）
        refreshCoordinator.notifyPending(null);
        clearSession('unauthorized');
        return null;
    } finally {
        refreshCoordinator.setIsRefreshing(false);
    }
}

export function setSession(token: string, user: User): void {
    useAuthStore.getState().login(user, token);
    refreshCoordinator.markLogin();
    emitSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    useAuthStore.getState().logout();
    emitSessionChange(reason);
}

export async function logout(): Promise<void> {
    try {
        // logout 需认证（已移出 PUBLIC_ENDPOINTS）：access 过期时由 request 层自动
        // 刷新并重试，保证能吊销 refresh 会话 + 清 cookie。
        await request('/auth/logout', { method: 'POST' });
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
    const redirectQuery = currentPath && currentPath !== '/' ? { redirect: currentPath } : undefined;
    const loginPath = redirectQuery ? `/login?redirect=${encodeURIComponent(redirectQuery.redirect)}` : '/login';
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

/**
 * 启动时恢复会话：access token 仅存内存，页面刷新后 token 缺失。
 * 若 HttpOnly refresh cookie 有效则刷新 access 并拉取用户，恢复登录态；否则清空会话。
 * <p>
 * 单飞：main.tsx 与 ProtectedRoute 会同时触发，并发进入时第二次的 /users/me 会撞上
 * requestManager 去重窗口被判为重复请求而抛错，错误分支会把刚恢复的会话又清掉
 * （表现为整页刷新后被踢回登录页），因此复用同一次恢复过程。
 */
export function restoreSession(): Promise<void> {
    sessionRestoreInFlight ??= doRestoreSession().finally(() => {
        sessionRestoreInFlight = null;
    });
    return sessionRestoreInFlight;
}

async function doRestoreSession(): Promise<void> {
    const store = useAuthStore.getState();
    if (store.token && !isTokenExpired(store.token)) {
        return;
    }

    const accessToken = await refreshAccessToken();
    if (!accessToken) {
        clearSession('unauthorized');
        return;
    }

    try {
        const response = await request<User>('/users/me');
        if (response.data) {
            setSession(accessToken, response.data);
        } else {
            clearSession('unauthorized');
        }
    } catch {
        clearSession('unauthorized');
    }
}
