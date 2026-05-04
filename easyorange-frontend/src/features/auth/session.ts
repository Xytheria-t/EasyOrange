import { storage } from '../../utils/index.js';
import { request } from '../../api/core/request.js';
import { useAuthStore } from '../../store/authStore.js';

const TOKEN_STORAGE_KEY = 'token';
const TOKEN_EXPIRES_KEY = 'token_expires';
const USER_STORAGE_KEY = 'user';

const TOKEN_EXPIRES_IN_MINUTES = 30;
const TOKEN_REFRESH_BEFORE_MINUTES = 5;

export const AUTH_SESSION_CHANGE_EVENT = 'auth-session-change';

export interface AuthSessionUser {
    username?: string;
    nickname?: string;
    [key: string]: unknown;
}

export type AuthSessionClearReason = 'logout' | 'unauthorized' | 'manual';

export interface AuthSessionDetail {
    isAuthenticated: boolean;
    token: string | null;
    user: AuthSessionUser | null;
    reason?: AuthSessionClearReason;
}

let unauthorizedRedirectInFlight = false;
let refreshTimer: ReturnType<typeof setTimeout> | null = null;
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];
let lastLoginTimestamp = 0;
const LOGIN_GRACE_PERIOD_MS = 5000;

export function subscribeTokenRefresh(callback: (token: string) => void): () => void {
    refreshSubscribers.push(callback);
    return () => {
        refreshSubscribers = refreshSubscribers.filter(cb => cb !== callback);
    };
}

function notifySubscribers(token: string): void {
    refreshSubscribers.forEach(cb => cb(token));
    refreshSubscribers = [];
}

function emitAuthSessionChange(reason?: AuthSessionClearReason): void {
    const detail: AuthSessionDetail = {
        isAuthenticated: Boolean(getStoredToken() && getStoredUser()),
        token: getStoredToken(),
        user: getStoredUser(),
        reason
    };

    window.dispatchEvent(new CustomEvent<AuthSessionDetail>(AUTH_SESSION_CHANGE_EVENT, { detail }));
}

export function getStoredToken(): string | null {
    const zustandToken = useAuthStore.getState().token;
    if (zustandToken) { return zustandToken; }
    return storage.get<string | null>(TOKEN_STORAGE_KEY, null);
}

export function getStoredUser(): AuthSessionUser | null {
    const zustandUser = useAuthStore.getState().user;
    if (zustandUser) { return zustandUser as unknown as AuthSessionUser; }
    return storage.get<AuthSessionUser | null>(USER_STORAGE_KEY, null);
}

export function getStoredTokenExpires(): number | null {
    return storage.get<number | null>(TOKEN_EXPIRES_KEY, null);
}

function scheduleTokenRefresh(expiresAt: number): void {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }

    const now = Date.now();
    const refreshTime = expiresAt - TOKEN_REFRESH_BEFORE_MINUTES * 60 * 1000;
    const delay = refreshTime - now;

    if (delay <= 0) {
        refreshAccessToken();
        return;
    }

    refreshTimer = setTimeout(() => {
        refreshAccessToken();
    }, delay);
}

function cancelScheduledRefresh(): void {
    if (refreshTimer) {
        clearTimeout(refreshTimer);
        refreshTimer = null;
    }
}

function syncAllStores(token: string, user: AuthSessionUser, expiresAt?: number, refreshToken?: string): void {
    storage.set(TOKEN_STORAGE_KEY, token);
    storage.set(USER_STORAGE_KEY, user);

    const finalRefreshToken = refreshToken ?? useAuthStore.getState().refreshToken ?? '';
    useAuthStore.getState().login(
        user as unknown as import('../../types/index.js').User,
        token,
        finalRefreshToken
    );

    if (expiresAt) {
        storage.set(TOKEN_EXPIRES_KEY, expiresAt);
        scheduleTokenRefresh(expiresAt);
    } else {
        storage.remove(TOKEN_EXPIRES_KEY);
        cancelScheduledRefresh();
    }
}

function clearAllStores(reason: AuthSessionClearReason = 'manual'): void {
    storage.remove(TOKEN_STORAGE_KEY);
    storage.remove(TOKEN_EXPIRES_KEY);
    storage.remove(USER_STORAGE_KEY);
    cancelScheduledRefresh();
    emitAuthSessionChange(reason);
}

export async function refreshAccessToken(): Promise<string | null> {
    const token = getStoredToken();
    if (!token) {
        return null;
    }

    if (isRefreshing) {
        return new Promise(resolve => {
            subscribeTokenRefresh(resolve);
        });
    }

    isRefreshing = true;

    try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const response = await request<string>('/auth/refresh', {
            method: 'POST',
            body: { refreshToken },
            skipAuth: true,
            timeout: 8000,
            retries: 0,
            dedupe: false
        });

        if (!response.data) {
            throw new Error('Refresh response invalid');
        }

        const expiresAt = Date.now() + TOKEN_EXPIRES_IN_MINUTES * 60 * 1000;
        const user = getStoredUser();
        if (user) {
            syncAllStores(response.data, user, expiresAt);
        } else {
            syncAllStores(response.data, { username: '', nickname: '' }, expiresAt);
        }
        notifySubscribers(response.data);

        return response.data;
    } catch {
        clearAllStores('unauthorized');
        useAuthStore.setState({
            user: null,
            token: null,
            refreshToken: null,
            isAuthenticated: false,
        });
        return null;
    } finally {
        isRefreshing = false;
    }
}

export function setSession(token: string, user: AuthSessionUser, expiresAt?: number, refreshToken?: string): void {
    syncAllStores(token, user, expiresAt, refreshToken);
    lastLoginTimestamp = Date.now();
    unauthorizedRedirectInFlight = false;
    emitAuthSessionChange();
}

export function clearSession(reason: AuthSessionClearReason = 'manual'): void {
    clearAllStores(reason);
}

export async function logout(): Promise<void> {
    const token = getStoredToken();

    if (token) {
        try {
            await request('/auth/logout', {
                method: 'POST'
            });
        } catch {
            // Ignore API errors - ensure local session is cleared regardless
        }
    }

    useAuthStore.getState().logout();
    clearSession('logout');
}

export function handleUnauthorized(): void {
    if (unauthorizedRedirectInFlight) {
        return;
    }

    const timeSinceLogin = Date.now() - lastLoginTimestamp;
    if (lastLoginTimestamp > 0 && timeSinceLogin < LOGIN_GRACE_PERIOD_MS) {
        return;
    }

    unauthorizedRedirectInFlight = true;
    useAuthStore.getState().logout();
    clearSession('unauthorized');

    const currentPath = `${window.location.pathname}${window.location.search}`;
    const redirectQuery = currentPath && currentPath !== '/' ? { redirect: currentPath } : undefined;
    const loginPath = redirectQuery
        ? `/login?redirect=${encodeURIComponent(redirectQuery.redirect)}`
        : '/login';
    window.location.href = loginPath;
}

export function initTokenRefresh(): void {
    const expiresAt = getStoredTokenExpires();
    if (expiresAt && expiresAt > Date.now()) {
        scheduleTokenRefresh(expiresAt);
    }
}

export function syncTokenToStore(token: string): void {
    useAuthStore.getState().setToken(token);
}
